package scorex.lagonaki.integration.api

import org.scalatest.{FunSuite, Matchers}
import play.api.libs.json.JsValue
import scorex.crypto.encode.Base58
import scorex.lagonaki.{TestingCommons, TransactionTestingCommons}
import scorex.transaction.BlockChain


class BlockAPISpecification extends FunSuite with Matchers with TransactionTestingCommons {

  import TestingCommons._

  val history = application.blockStorage.history
  val genesis = history.genesis

  while (history.height() < 3) {
    application.blockStorage.appendBlock(genValidBlock())
  }
  val last = history.lastBlock

  test("GET /blocks/at/{height} API route") {
    val response = GET.request(s"/blocks/at/1")
    checkGenesis(response)
  }

  test("GET /blocks/seq/{from}/{to} API route") {
    val response = GET.request(s"/blocks/seq/1/3")
    checkGenesis(response(0).as[JsValue])
    checkBlock(response(1).as[JsValue])
    (response(1) \ "height").as[Int] shouldBe 2
    (response(2) \ "height").as[Int] shouldBe 3
  }

  test("GET /blocks/last API route") {
    val response = GET.request(s"/blocks/last")
    checkBlock(response)
  }

  test("GET /blocks/height API route") {
    val response = GET.request(s"/blocks/height")
    (response \ "height").as[Int] shouldBe history.height()
  }

  test("GET /blocks/child/{signature} API route") {
    val response = GET.request(s"/blocks/child/${genesis.encodedId}")
    checkBlock(response)
    (response \ "signature").as[String] shouldBe history.asInstanceOf[BlockChain].blockAt(2).get.encodedId
  }

  test("GET /blocks/delay/{signature}/{blockNum} API route") {
    val response = GET.request(s"/blocks/delay/${last.encodedId}/1")
    (response \ "delay").as[Long] should be > 0L
  }

  test("GET /blocks/height/{signature} API route") {
    val response = GET.request(s"/blocks/height/${genesis.encodedId}")
    (response \ "height").as[Int] shouldBe 1
  }

  test("GET /blocks/signature/{signature} API route") {
    Base58.decode(genesis.encodedId).toOption.map(signature => history.blockById(signature)).isDefined shouldBe true
    checkGenesis(GET.request(s"/blocks/signature/${genesis.encodedId}"))
    checkBlock(GET.request(s"/blocks/signature/${last.encodedId}"))
  }

  test("GET /blocks/first API route") {
    checkGenesis(GET.request(s"/blocks/first"))
  }

  test("GET /blocks/address/{address}/{from}/{to} API route") {
    checkGenesis(GET.request(s"/blocks/address/3Mc2PfwgwZ6txN2rhi6DzYfJRLQ88xRLx5p/0/1")(0).as[JsValue])
  }


  def checkGenesis(response: JsValue): Unit = {
    (response \ "reference").as[String] shouldBe "67rpwLCuS5DGA8KGZXKsVQ7dnPb9goRLoKfgGbLfQg9WoLUgNY77E2jT11fem3coV9nAkguBACzrU1iyZM4B8roQ"
    (response \ "transactions" \\ "fee").toList.size shouldBe 3
    (response \ "generator").as[String] shouldBe "3Mc2PfwgwZ6txN2rhi6DzYfJRLQ88xRLx5p"
    (response \ "signature").as[String] shouldBe "1111111111111111111111111111111111111111111111111111111111111111"
    (response \ "fee").as[Int] shouldBe 0
    checkBlock(response)
  }

  def checkBlock(response: JsValue): Unit = {
    (response \ "version").asOpt[Int].isDefined shouldBe true
    (response \ "timestamp").as[Long] should be >= 0L
    (response \ "reference").asOpt[String].isDefined shouldBe true
    (response \ "transactions" \\ "fee").toList.size should be >= 0
    (response \ "generator").asOpt[String].isDefined shouldBe true
    (response \ "signature").asOpt[String].isDefined shouldBe true
    (response \ "fee").as[Int] should be >= 0
    (response \ "blocksize").as[Int] should be > 0
  }

}