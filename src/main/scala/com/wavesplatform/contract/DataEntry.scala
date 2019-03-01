package com.wavesplatform.contract

import play.api.libs.json._

sealed abstract class DataEntry[T](val `type`: String, val key: String, val value: T) {
  def toJson: JsObject = Json.obj("key" -> key, "type" -> `type`)
}

object DataEntry {

  object Type extends Enumeration {
    val Integer: Type.Value = Value(0)
    val Boolean: Type.Value = Value(1)
    val Binary: Type.Value = Value(2)
    val String: Type.Value = Value(3)
  }

  implicit object Format extends Format[DataEntry[_]] {
    def reads(jsv: JsValue): JsResult[DataEntry[_]] = {
      jsv \ "key" match {
        case JsDefined(JsString(key)) =>
          jsv \ "type" match {
            case JsDefined(JsString("integer")) =>
              jsv \ "value" match {
                case JsDefined(JsNumber(n)) => JsSuccess(IntegerDataEntry(key, n.toLong))
                case _ => JsError("value is missing or not an integer")
              }
            case JsDefined(JsString("boolean")) =>
              jsv \ "value" match {
                case JsDefined(JsBoolean(b)) => JsSuccess(BooleanDataEntry(key, b))
                case _ => JsError("value is missing or not a boolean value")
              }
            case JsDefined(JsString("string")) =>
              jsv \ "value" match {
                case JsDefined(JsString(str)) => JsSuccess(StringDataEntry(key, str))
                case _ => JsError("value is missing or not a string")
              }
            case JsDefined(JsString(t)) => JsError(s"unknown type $t")
            case _ => JsError("type is missing")
          }
        case _ => JsError("key is missing")
      }
    }

    def writes(item: DataEntry[_]): JsValue = item.toJson
  }

}

case class IntegerDataEntry(override val key: String, override val value: Long) extends DataEntry[Long]("integer", key, value) {
  override def toJson: JsObject = super.toJson + ("value" -> JsNumber(value))
}

case class BooleanDataEntry(override val key: String, override val value: Boolean) extends DataEntry[Boolean]("boolean", key, value) {
  override def toJson: JsObject = super.toJson + ("value" -> JsBoolean(value))
}

case class StringDataEntry(override val key: String, override val value: String) extends DataEntry[String]("string", key, value) {
  override def toJson: JsObject = super.toJson + ("value" -> JsString(value))
}
