package testminer.parser

import java.nio.file.Files
import java.nio.file.Path

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

import org.eclipse.jdt.core._
import org.eclipse.jdt.core.dom._

object EclipseParser {

  import testminer._

  val Unresolved = "unresolved"

  def signature(method: IMethodBinding): Signature = {

    val parTypes = method.getParameterTypes

    if (method.isParameterizedMethod || method.isRawMethod) {
      val typeArgs = method.getTypeArguments
      Signature(method.getName, typeArgs.map(_.getQualifiedName).toList, parTypes.map(_.getQualifiedName).toList)
    } else {
      Signature(method.getName, List.empty[String], parTypes.map(_.getQualifiedName).toList)
    }

  }

  def signature(declaration: MethodDeclaration, binding: IMethodBinding): Signature = {

    val parNames = declaration
      .parameters
      .map(param => param.asInstanceOf[SingleVariableDeclaration])
      .map(_.getName.toString)
      .toList

    val parTypes = binding.getParameterTypes

    if (binding.isParameterizedMethod || binding.isRawMethod) {
      val typeArgs = binding.getTypeArguments
      Signature(binding.getName, typeArgs.map(_.getQualifiedName).toList, parTypes.map(_.getQualifiedName).toList, parNames)
    } else {
      Signature(binding.getName, List.empty[String], parTypes.map(_.getQualifiedName).toList, parNames)
    }

  }

  def reference(tb: ITypeBinding): Reference = {
    val qualifiedName = tb.getQualifiedName.split("<").head
    if (tb.isParameterizedType || tb.isRawType) {
      val typeArgs = tb.getTypeArguments
      Reference(qualifiedName, typeArgs.map(_.getQualifiedName).toList)
    } else {
      Reference(qualifiedName, List.empty[String])
    }
  }

  var numberOfUnresolvedTuples = 0
  var numberOfResolvedTuples = 0

  def parserForSource(filePath: Path, jarsPath: List[String], sourcesPath: List[String]): ASTParser = {

    val parser = ASTParser.newParser(AST.JLS8)

    val sourceCode = new String(Files.readAllBytes(filePath))
    parser.setSource(sourceCode.toCharArray)
    parser.setKind(ASTParser.K_COMPILATION_UNIT)
    parser.setResolveBindings(true)
    parser.setBindingsRecovery(true)
    parser.setStatementsRecovery(true)
    parser.setUnitName(filePath.getFileName.toString)
    parser.setEnvironment(
      (List("libs/java/7/rt.jar") ++ jarsPath).toArray,
      sourcesPath.toArray,
      null, false)

    val options = JavaCore.getOptions
    JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options)
    parser.setCompilerOptions(options)

    parser

  }

  def types(filePath: Path, jarsPath: List[String], sourcesPath: List[String]): List[Type] = {

    val parser = parserForSource(filePath, jarsPath, sourcesPath)
    val cu = parser.createAST(null).asInstanceOf[CompilationUnit]

    /*
    cu.getProblems.foreach(item => println(" " + item)) 
    if (cu.getAST().hasResolvedBindings) {
      println("<Binding activated>")
    } else {
      println("<Binding *NOT* activated>")
    } 
    */

    val types = ArrayBuffer.empty[Type]

    cu.accept(new ASTVisitor {

      override def endVisit(node: TypeDeclaration): Unit = {

        val typeBinding = node.resolveBinding

        if (typeBinding != null) {
          val methods = ArrayBuffer.empty[Function]
          node.accept(new ASTVisitor {
            override def endVisit(methodNode: MethodDeclaration): Unit = {
              val bind = methodNode.resolveBinding
              if (bind != null) {
                val mv = new MethodAstVisitor
                methodNode.accept(mv)
                mv.result.foreach {
                  case UnresolvedCall(_, _, _)
                  => numberOfUnresolvedTuples += 1
                  case Call(_, _, _)
                  => numberOfResolvedTuples += 1
                  case _ =>
                }
                if (methodNode.isConstructor) {
                  methods += Constructor(signature(methodNode, bind), mv.result.toList)
                } else {
                  methods += Method(signature(methodNode, bind), mv.result.toList)
                }
              }
            }
          })

          if (typeBinding.isInterface) {
            println("  interface " + typeBinding.getQualifiedName)
            types += Interface(typeBinding.getQualifiedName)
          } else {
            println("  type " + typeBinding.getQualifiedName)
            types += Class(typeBinding.getQualifiedName, typeBinding.getSuperclass.getQualifiedName, methods.toList)
          }

        }
      }
    })

    types.toList

  }

  class MethodAstVisitor extends ASTVisitor {

    val result: ArrayBuffer[Tuple] = ArrayBuffer.empty[Tuple]

    override def endVisit(node: ClassInstanceCreation): Unit = {
      val ctorBinding = node.resolveConstructorBinding
      if (ctorBinding != null) {

        val ctorClass = ctorBinding.getDeclaringClass
        val ctorArgs = node.arguments.map { arg =>
          val exprArg = arg.asInstanceOf[Expression]
          val v = new LiteralAstVisitor
          exprArg.accept(v)
          v.result
        }.toList

        result += NewObject(reference(ctorClass), ctorArgs)

      }
    }

    override def endVisit(node: MethodInvocation): Unit = {

      val mthBinding = node.resolveMethodBinding
      if (mthBinding != null) {
        val mthArgs = node.arguments.map { arg =>
          val exprArg = arg.asInstanceOf[Expression]
          val v = new LiteralAstVisitor
          exprArg.accept(v)
          v.result
        }.toList

        result += Call(
          reference(mthBinding.getDeclaringClass),
          signature(mthBinding),
          mthArgs)

      }
    }

  }

  class LiteralAstVisitor extends ASTVisitor {

    override def visit(node: ArrayAccess): Boolean = {
      false
    }

    override def visit(node: ArrayCreation): Boolean = {
      false
    }

    override def visit(node: ArrayInitializer): Boolean = {
      false
    }

    override def visit(node: AssertStatement): Boolean = {
      false
    }

    override def visit(node: Assignment): Boolean = {
      false
    }

    override def visit(node: Block): Boolean = {
      false
    }

    override def visit(node: BlockComment): Boolean = {
      false
    }

    override def visit(node: BreakStatement): Boolean = {
      false
    }

    override def visit(node: CastExpression): Boolean = {
      false
    }

    override def visit(node: CatchClause): Boolean = {
      false
    }

    override def visit(node: ClassInstanceCreation): Boolean = {
      false
    }

    override def visit(node: CompilationUnit): Boolean = {
      false
    }

    override def visit(node: ConditionalExpression): Boolean = {
      false
    }

    override def visit(node: ConstructorInvocation): Boolean = {
      false
    }

    override def visit(node: ContinueStatement): Boolean = {
      false
    }

    override def visit(node: CreationReference): Boolean = {
      false
    }

    override def visit(node: Dimension): Boolean = {
      false
    }

    override def visit(node: DoStatement): Boolean = {
      false
    }

    override def visit(node: EmptyStatement): Boolean = {
      false
    }

    override def visit(node: EnhancedForStatement): Boolean = {
      false
    }

    override def visit(node: EnumConstantDeclaration): Boolean = {
      false
    }

    override def visit(node: EnumDeclaration): Boolean = {
      false
    }

    override def visit(node: ExpressionMethodReference): Boolean = {
      false
    }

    override def visit(node: ExpressionStatement): Boolean = {
      false
    }

    override def visit(node: FieldAccess): Boolean = {
      false
    }

    override def visit(node: FieldDeclaration): Boolean = {
      false
    }

    override def visit(node: ForStatement): Boolean = {
      false
    }

    override def visit(node: IfStatement): Boolean = {
      false
    }

    override def visit(node: ImportDeclaration): Boolean = {
      false
    }

    override def visit(node: InfixExpression): Boolean = {
      false
    }

    override def visit(node: Initializer): Boolean = {
      false
    }

    override def visit(node: InstanceofExpression): Boolean = {
      false
    }

    override def visit(node: IntersectionType): Boolean = {
      false
    }

    override def visit(node: Javadoc): Boolean = {
      false
    }

    override def visit(node: LabeledStatement): Boolean = {
      false
    }

    override def visit(node: LambdaExpression): Boolean = {
      false
    }

    override def visit(node: LineComment): Boolean = {
      false
    }

    override def visit(node: MarkerAnnotation): Boolean = {
      false
    }

    override def visit(node: MemberRef): Boolean = {
      false
    }

    override def visit(node: MemberValuePair): Boolean = {
      false
    }

    override def visit(node: MethodDeclaration): Boolean = {
      false
    }

    override def visit(node: MethodInvocation): Boolean = {
      false
    }

    override def visit(node: MethodRef): Boolean = {
      false
    }

    override def visit(node: MethodRefParameter): Boolean = {
      false
    }

    override def visit(node: Modifier): Boolean = {
      false
    }

    override def visit(node: NameQualifiedType): Boolean = {
      false
    }

    override def visit(node: NormalAnnotation): Boolean = {
      false
    }

    override def visit(node: PackageDeclaration): Boolean = {
      false
    }

    override def visit(node: ParameterizedType): Boolean = {
      false
    }

    override def visit(node: ParenthesizedExpression): Boolean = {
      false
    }

    override def visit(node: PostfixExpression): Boolean = {
      false
    }

    override def visit(node: PrefixExpression): Boolean = {
      false
    }

    override def visit(node: PrimitiveType): Boolean = {
      false
    }

    override def visit(node: QualifiedName): Boolean = {
      false
    }

    override def visit(node: ReturnStatement): Boolean = {
      false
    }

    override def visit(node: SimpleName): Boolean = {
      false
    }

    override def visit(node: SimpleType): Boolean = {
      false
    }

    override def visit(node: SingleMemberAnnotation): Boolean = {
      false
    }

    override def visit(node: SingleVariableDeclaration): Boolean = {
      false
    }

    override def visit(node: SuperConstructorInvocation): Boolean = {
      false
    }

    override def visit(node: SuperFieldAccess): Boolean = {
      false
    }

    override def visit(node: SuperMethodInvocation): Boolean = {
      false
    }

    override def visit(node: SuperMethodReference): Boolean = {
      false
    }

    override def visit(node: SwitchCase): Boolean = {
      false
    }

    override def visit(node: SwitchStatement): Boolean = {
      false
    }

    override def visit(node: SynchronizedStatement): Boolean = {
      false
    }

    override def visit(node: TagElement): Boolean = {
      false
    }

    override def visit(node: TextElement): Boolean = {
      false
    }

    override def visit(node: ThisExpression): Boolean = {
      false
    }

    override def visit(node: ThrowStatement): Boolean = {
      false
    }

    override def visit(node: TryStatement): Boolean = {
      false
    }

    override def visit(node: TypeDeclaration): Boolean = {
      false
    }

    override def visit(node: TypeDeclarationStatement): Boolean = {
      false
    }

    override def visit(node: TypeMethodReference): Boolean = {
      false
    }

    override def visit(node: TypeParameter): Boolean = {
      false
    }

    override def visit(node: UnionType): Boolean = {
      false
    }

    override def visit(node: VariableDeclarationExpression): Boolean = {
      false
    }

    override def visit(node: VariableDeclarationFragment): Boolean = {
      false
    }

    override def visit(node: VariableDeclarationStatement): Boolean = {
      false
    }

    override def visit(node: WhileStatement): Boolean = {
      false
    }

    override def visit(node: WildcardType): Boolean = {
      false
    }

    var result: Term = Unknown

    def getConstForType(value: Object, binding: ITypeBinding): Term = {

      binding.toString match {
        case "int" =>
          IntConst(value.asInstanceOf[Int])
        case "long" =>
          LongConst(value.asInstanceOf[Long])
        case "short" =>
          ShortConst(value.asInstanceOf[Short])
        case "byte" =>
          ByteConst(value.asInstanceOf[Byte])
        case "float" =>
          FloatConst(value.asInstanceOf[Float])
        case "double" =>
          DoubleConst(value.asInstanceOf[Double])
        case "void" =>
          Unknown
        case _ =>
          throw new RuntimeException
      }

    }

    override def visit(node: NullLiteral): Boolean = {
      result = NullConst
      false
    }

    override def visit(node: NumberLiteral): Boolean = {
      val value = node.resolveConstantExpressionValue
      if (value != null) {
        val typeBinding = node.resolveTypeBinding
        if (typeBinding != null) {
          result = getConstForType(value, typeBinding)
        }
      }
      false
    }

    override def visit(node: StringLiteral): Boolean = {
      result = StringConst(node.getLiteralValue)
      false
    }

    override def visit(node: BooleanLiteral): Boolean = {
      result = BooleanConst(node.booleanValue)
      false
    }

    override def visit(node: CharacterLiteral): Boolean = {
      result = CharConst(node.getEscapedValue)
      false
    }

    override def visit(node: TypeLiteral): Boolean = {
      val binding = node.getType.resolveBinding
      if (binding != null) {
        result = ClassTypeConst(binding.getQualifiedName)
      }
      false
    }

  }

}