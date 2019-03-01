package com.wavesplatform.contract

import com.softwaremill.sttp.StatusCodes
import com.softwaremill.sttp.quick._
import play.api.libs.json._

/**
  * Voting contract
  */
object Main extends App {

  import CreateTx._

  val nodePort = System.getenv("NODE_PORT")
  require(nodePort != null)
  val command = System.getenv("COMMAND")
  require(command != null)
  val tx = System.getenv("TX")
  require(tx != null)

  command match {
    case "CREATE" =>
      val createTx = Json.parse(tx).as[CreateTx]
      handleCreateTx(createTx)
    case "CALL" =>
      val callTx = Json.parse(tx).as[CallTx]
      handleVote(callTx)
  }

  private def handleCreateTx(createTx: CreateTx): Unit = {
    val candidates = createTx.candidates()
    println("Create tx is called")
    val results = candidates.map(IntegerDataEntry(_, 0))
    println(Json.toJson(results))
  }

  private def handleVote(callTx: CallTx): Unit = {
    println("Call tx is called")
    checkIsVoted(callTx)
    val req = sttp.get(uri"http://node:$nodePort/contracts/${callTx.contractId}/${callTx.candidate()}").send()
    if (req.code == StatusCodes.Ok) {
      val candidate = Json.parse(req.body.right.get).as[DataEntry[_]].asInstanceOf[IntegerDataEntry]
      val newVotes = IntegerDataEntry(candidate.key, candidate.value + 1)
      val voted = BooleanDataEntry(callTx.voter(), value = true)
      println(Json.toJson(Array(newVotes, voted)))
    } else if (req.code == StatusCodes.NotFound) {
      throw new RuntimeException(s"Candidate with name ${callTx.candidate()} is not found")
    } else {
      throw new IllegalStateException("Can't read value from the node")
    }
  }

  private def checkIsVoted(callTx: CallTx): Unit = {
    val req = sttp.get(uri"http://node:$nodePort/contracts/${callTx.contractId}/${callTx.voter()}").send()
    if (req.code == StatusCodes.Ok) {
      val vote = Json.parse(req.body.right.get).as[DataEntry[_]].asInstanceOf[BooleanDataEntry]
      if (vote.value) {
        throw new RuntimeException(s"Voter with name ${callTx.voter()} has already voted")
      }
    } else if (req.code == StatusCodes.NotFound) {
      // voter has not already voted
    } else {
      throw new IllegalStateException("Can't read value from the node")
    }
  }
}

case class CreateTx(version: Byte,
                    sender: String,
                    senderPublicKey: String,
                    image: String,
                    imageHash: String,
                    params: List[DataEntry[_]],
                    fee: Long,
                    timestamp: Long) {

  def candidates(): Seq[String] = Json.parse(params.find(_.key == "candidates").get.asInstanceOf[StringDataEntry].value).asInstanceOf[JsArray]
    .value.map(_.asInstanceOf[JsString].value)

}

object CreateTx {
  implicit val format: OFormat[CreateTx] = Json.format
}

case class CallTx(version: Byte,
                  sender: String,
                  senderPublicKey: String,
                  contractId: String,
                  params: List[DataEntry[_]],
                  fee: Long,
                  timestamp: Long) {

  def candidate(): String = params.find(_.key == "candidate").get.asInstanceOf[StringDataEntry].value

  def voter(): String = params.find(_.key == "voter").get.asInstanceOf[StringDataEntry].value

}

object CallTx {
  implicit val format: OFormat[CallTx] = Json.format
}
