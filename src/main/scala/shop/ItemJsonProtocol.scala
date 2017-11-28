package shop;

import java.net.URI

import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat, deserializationError}

object ItemJsonProtocol extends DefaultJsonProtocol {
  implicit object URIJsonFormat extends RootJsonFormat[URI] {
    def write(uri: URI) = JsString(uri.toString)

    def read(value: JsValue) = value match {
      case JsString(uri) => URI.create(uri)
      case _ => deserializationError("URI expected")
    }
  }
}