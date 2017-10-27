package testminer

import spray.json._

object ExportJsonProtocol extends DefaultJsonProtocol {

  implicit object TermFormat extends JsonFormat[Term] {

    def write(term: Term): JsObject = term match {
      case NullConst => JsObject("type" -> JsString("null"))
      case v: StringConst => JsObject("type" -> JsString("string"), "value" -> JsString(v.value))
      case n: ClassTypeConst => JsObject("type" -> JsString("type"), "value" -> JsString(n.name))
      case c: CharConst => JsObject("type" -> JsString("char"), "value" -> JsString(c.value))
      case c: IntConst => JsObject("type" -> JsString("int"), "value" -> JsNumber(c.value))
      case c: LongConst => JsObject("type" -> JsString("long"), "value" -> JsNumber(c.value))
      case c: ShortConst => JsObject("type" -> JsString("short"), "value" -> JsNumber(c.value))
      case c: ByteConst => JsObject("type" -> JsString("byte"), "value" -> JsNumber(c.value))
      case c: FloatConst => JsObject("type" -> JsString("float"), "value" -> JsNumber(c.value))
      case c: DoubleConst => JsObject("type" -> JsString("double"), "value" -> JsNumber(c.value))
      case c: BooleanConst => JsObject("type" -> JsString("boolean"), "value" -> JsBoolean(c.value))
      case v: Variable => JsObject("type" -> JsString("var"), "name" -> JsString(v.name), "value_type" -> JsString(v.typeName))
      case Unknown => JsObject("type" -> JsString("unk"))
    }

    def read(value: JsValue): Term = {
      null
    }

  }

  implicit object ReferenceFormat extends JsonFormat[Reference] {

    def write(r: Reference): JsObject = {
      JsObject(
        "name" -> JsString(r.name),
        "type_args" -> r.typeArgs.toJson
      )
    }

    def read(value: JsValue): Reference = {
      null
    }

  }

  implicit object SignatureFormat extends JsonFormat[Signature] {

    def write(s: Signature): JsObject = {
      JsObject(
        "name" -> JsString(s.name),
        "type_args" -> s.typeArgs.toJson,
        "params" -> s.params.toJson,
        "params_names" -> s.paramsNames.toJson)
    }

    def read(value: JsValue): Signature = {
      null
    }

  }

  implicit object UnresolvedCallFormat extends JsonFormat[UnresolvedCall] {

    def write(c: UnresolvedCall): JsObject = {
      JsObject(
        "type" -> JsString("unresolved_call"),
        "target_type" -> c.targetType.toJson,
        "target_method" -> c.targetMethod.toJson,
        "values" -> c.args.toJson)
    }

    def read(value: JsValue): UnresolvedCall = {
      null
    }

  }

  implicit object UnresolvedNewObjectFormat extends JsonFormat[UnresolvedNewObject] {

    def write(n: UnresolvedNewObject): JsObject = {
      JsObject(
        "type" -> JsString("unresolved_alloc"),
        "alloc_type" -> n.allocType.toJson,
        "values" -> n.args.toJson)
    }

    def read(value: JsValue): UnresolvedNewObject = {
      null
    }

  }

  implicit object CallFormat extends JsonFormat[Call] {

    def write(c: Call): JsObject = {
      JsObject(
        "type" -> JsString("call"),
        "target_type" -> c.targetType.toJson,
        "target_method" -> c.targetMethod.toJson,
        "values" -> c.args.toJson)
    }

    def read(value: JsValue): Call = {
      null
    }

  }

  implicit object NewObjectFormat extends JsonFormat[NewObject] {

    def write(n: NewObject): JsObject = {
      JsObject(
        "type" -> JsString("alloc"),
        "alloc_type" -> n.allocType.toJson,
        "values" -> n.args.toJson)
    }

    def read(value: JsValue): NewObject = {
      null
    }

  }

  implicit object TupleFormat extends JsonFormat[Tuple] {

    def write(t: Tuple): JsValue = t match {
      case uc: UnresolvedCall => uc.toJson
      case c: Call => c.toJson
      case n: NewObject => n.toJson
      case un: UnresolvedNewObject => un.toJson
    }

    def read(value: JsValue): Tuple = {
      null
    }

  }

  implicit object FunctionFormat extends JsonFormat[Function] {

    def write(fun: Function): JsObject = fun match {
      case m: Method => JsObject(
        "signature" -> m.signature.toJson,
        "tuples" -> m.tuples.toJson)
      case c: Constructor => JsObject(
        "signature" -> c.signature.toJson,
        "tuples" -> c.tuples.toJson)
    }

    def read(value: JsValue): Function = {
      null
    }

  }

  implicit object TypeFormat extends JsonFormat[Type] {

    def write(t: Type): JsValue = t match {
      case c: Class => JsObject(
        "class" -> JsString(c.name),
        "super_class" -> JsString(c.superName),
        "methods" -> c.methods.toJson)
      case i: Interface =>
        JsObject("interface" -> JsString(i.name))
      case r: Reference =>
        r.toJson
    }

    def read(value: JsValue): Type = {
      null
    }

  }

}