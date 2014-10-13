package es.care.sf.scraper.bank.branch.domain

import reactivemongo.bson.BSONDocumentReader
import reactivemongo.bson.BSONDocument

object Bank{
  
  implicit object PersonReader extends BSONDocumentReader[Bank] {
    def read(doc: BSONDocument): Bank = {
      
      val bankId = doc.getAs[String]("bankId").get
      val name = doc.getAs[String]("name").get

      Bank(name, bankId)
    }
  }

}
case class Bank (name:String, bankId: String)