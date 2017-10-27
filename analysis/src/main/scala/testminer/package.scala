package object testminer {

  abstract class Type
  case class Class(name: String, superName: String, methods: List[Function]) extends Type
  case class Interface(name: String, methods: List[Function] = List.empty[Function]) extends Type
  case class Reference(name: String, typeArgs: List[String]) extends Type

  case class Signature(name: String, typeArgs: List[String], params: List[String], paramsNames: List[String] = List.empty[String])

  abstract class Function
  case class Method(signature: Signature, tuples: List[Tuple] = List.empty[Tuple]) extends Function
  case class Constructor(signature: Signature, tuples: List[Tuple] = List.empty[Tuple]) extends Function

  abstract class Tuple
  case class Call(targetType: Reference, targetMethod: Signature, args: List[Term]) extends Tuple
  case class NewObject(allocType: Reference, args: List[Term]) extends Tuple
  case class UnresolvedCall(targetType: Reference, targetMethod: Signature, args: List[Term]) extends Tuple
  case class UnresolvedNewObject(allocType: Reference, args: List[Term]) extends Tuple

  abstract class Term
  case class StringConst(value: String) extends Term
  case class CharConst(value: String) extends Term
  case class IntConst(value: Int) extends Term
  case class LongConst(value: Long) extends Term
  case class ShortConst(value: Short) extends Term
  case class ByteConst(value: Byte) extends Term
  case class FloatConst(value: Float) extends Term
  case class DoubleConst(value: Double) extends Term
  case class BooleanConst(value: Boolean) extends Term
  case class ClassTypeConst(name: String) extends Term
  case class Variable(name:String, typeName: String) extends Term
  case object NullConst extends Term
  case object Unknown extends Term
  case object Void extends Term
}
