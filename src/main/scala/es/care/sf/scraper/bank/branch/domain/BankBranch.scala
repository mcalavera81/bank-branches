package es.care.sf.scraper.bank.branch.domain

import reactivemongo.bson.BSONDocumentWriter
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONObjectID

object BankBranch{
	implicit object BankBranchWriter extends BSONDocumentWriter[BankBranch] {
  
		def write(branch: BankBranch): BSONDocument = {
				BSONDocument("_id" -> branch.id.getOrElse(BSONObjectID.generate),
				    "region" -> branch.region,
				    "province" -> branch.province,
				    "url" -> branch.url,
				    "bank" -> branch.bank,
				    "branchCode" -> branch.branchCode,
				    "postalCode" -> branch.postalCode,
				    "address" -> branch.address)
		}
			
	}
}

case class BankBranch(
	  id: Option[BSONObjectID] = None,	
      region: String,
      province: String,
      url: String,
      bank: String,
      branchCode: String,
      postalCode: Option[String],
      address: String)
