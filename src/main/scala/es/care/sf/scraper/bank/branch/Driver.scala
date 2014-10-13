package es.care.sf.scraper.bank.branch

import scala.Option.option2Iterable
import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import es.care.sf.scraper.bank.branch.domain.Bank
import reactivemongo.bson.BSONDocument
import reactivemongo.core.nodeset.Authenticate
import org.apache.commons.lang3.StringUtils
import es.care.sf.scraper.bank.branch.domain.BankBranch
import com.github.tototoshi.csv.CSVWriter
import java.io.File

object Driver {

  val ConnectionTimeout = 120000

  val rootUrl = "http://www.iahorro.com/oficinas/"

  val bankBranchRegEx = """Oficina\s+(\d{4})\s+(.+)""".r

  val postalCodeRegEx = """.*postal\s+(\d+).*""".r

  val regexRegionUrl = """.*/([A-Za-z\-_]+/\d*?/?)""".r

  def getDocument(url: String): Document = {
    Jsoup.connect(url).timeout(ConnectionTimeout).get()
  }

  def main(args: Array[String]): Unit = {

    val db = connect()

    /*val bank = db("bank")
    val allBanks = bank.find(BSONDocument())

    val resBanks = allBanks.cursor[Bank].collect[List]()
    val banks = Await.result(resBanks, 100 second)

    val map =getEntidades(banks)
    map.toSeq.foreach(println)*/

    val officePerPage = 15
    case class Link(url: String, name: String)


    def extractLink(element: Element): Link = {
      Link(element.attr("href"), element.text())
    }

    val doc = getDocument(rootUrl)

    

    val selectors = Map(
      "counter" -> "h1.title.grid_22.push_1 span",
      "regions" -> "ul#provsx a[href]",
      "office" -> "div.info_interior_fichas h2.h1_oficina a",
      "province" -> "div.marl10.wn290.floatz a:eq(0)",
      "region" -> "div.marl10.wn290.floatz a:eq(1)",
      "address" -> "div.martop10.marl10.wn290.floatz strong",
      "postalCode" -> "div.martop10.floatz:eq(4)")

    val baseRegionLinks = doc.select(selectors("regions")).map(extractLink).toList

    val branchesCountRegEx = """\((\d+).*\)""".r

    val regionLinks = baseRegionLinks.par.flatMap(regionLink => {
      regionLink.url match {

        case regexRegionUrl(region_relative_url) =>
          {
            val regionDoc = getDocument(rootUrl + region_relative_url)
            val counter = regionDoc.select(selectors("counter")).text() match {
              case branchesCountRegEx(x) => x.toInt
              case _ => {
                println("Something went wrong!")
                0
              }
            }

            val pages = Math.ceil(counter.toFloat / officePerPage).toInt
            if (pages > 1) {
              val pageRange = 2 to pages
              val pageUrls = pageRange.map(page => rootUrl + region_relative_url + page).toList
              regionLink.url :: pageUrls.toList
            } else {
              List(regionLink.url)
            }

          }
      }
    }).toList


    val listOfFutureResults = regionLinks.map(regionLink => Future {

      regionLink match {

        case regexRegionUrl(region_relative_url) => {
          val regionDoc = getDocument(rootUrl + region_relative_url)

          val province = regionDoc.select(selectors("province")).map(extractLink).toList
          val region = regionDoc.select(selectors("region")).map(extractLink).toList
          val office = regionDoc.select(selectors("office")).map(extractLink).toList
          val address = regionDoc.select(selectors("address")).toList map { _.text() }

          val postCode = regionDoc.select(selectors("postalCode")).toList map {
            element =>
              element.text() match {
                case postalCodeRegEx(x) => Some(x)
                case _ => {
                  None
                }
              }
          }

          office.zip(province).zip(region).zip(address).zip(postCode).map(
            (elem) => {
              elem._1._1._1._1.name match {
                case bankBranchRegEx(branchCode, bank) => {
                  Some(BankBranch(None, elem._1._1._2.name, elem._1._1._1._2.name, elem._1._1._1._1.url, bank, branchCode, elem._2, elem._1._2))
                }
                case _ => None
              }

            })
        }
        case _ => {
          println(s"Something went wrong!:${regionLink}")
          List()
        }
      }

    })

    val resultsFuture = Future.sequence(listOfFutureResults)

    val results = Await.result(resultsFuture, 100000 second).flatten.flatten

    
    
    val bankBranches = db("bankBranches")
    Await.result(bankBranches.drop,100 second)
    
    //val file = new File("aaa.csv")
    //val writer = CSVWriter.open(file)

    /*writer.writeRow(List())

    writer.writeRow(List("Region", "Province", "Url", "Bank", "BranchCode", "Address", "PostalCode"))*/
    results.foreach(result => {
      //writer.writeRow(List(result.region, result.province, result.url, result.bank, result.branchCode, result.address, result.postalCode.getOrElse("")))
      val future = bankBranches.insert(result)
      future.onFailure{ case e => println(s"Error inserting document ${result}: ${e.getMessage()}")}
    })

    //writer.close

    println("Hello, world!")

  }

  def connect() = {
    import reactivemongo.api._
    import scala.concurrent.ExecutionContext.Implicits.global

    // gets an instance of the driver
    // (creates an actor system)
    val driver = new MongoDriver
    val dbName = "business"
    val userName = "business"
    val password = "business"
    val credentials = List(Authenticate(dbName, userName, password))
    val connection = driver.connection(List("sdev7201"), authentications = credentials)

    // Gets a reference to the database "plugin"
    connection("business")
  }

  def getEntidades(banks: List[Bank]): Map[String, (Float,String)] = {
    val doc = getDocument(rootUrl)
    val entidades = doc.select("select#cmb_entidades option").toList.map { _.text() }.toList
    val mapping = entidades.map {
      entidad =>
        val bestMatch = banks.foldLeft[Tuple2[Float, String]](100, "") {
          (tuple, bank) =>
            {
              val distance = StringUtils.getLevenshteinDistance(bank.name, entidad).toFloat/Math.max(bank.name.length(), entidad.length())
              if (distance < tuple._1) (distance, bank.bankId) else tuple
            }
        }
        (entidad, (bestMatch._1, bestMatch._2))
    }

    mapping.toMap
  }
}


