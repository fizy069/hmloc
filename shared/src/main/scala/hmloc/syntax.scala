package hmloc

import hmloc.utils._, shorthands._

////////////////////////////////////////////////
// Terms
////////////////////////////////////////////////

final case class Pgrm(tops: Ls[Statement]) extends PgrmImpl

sealed trait Statement extends StatementImpl

sealed trait Terms extends Statement

sealed abstract class Decl extends Statement with DeclImpl
/** A Def is any function declaration in the global scope
  *
  * @param rec
  *   If the defintion is recursive
  * @param nme
  *   the name the body of the defintion is bound
  * @param rhs
  *   Is a term if it's a definition and a type if it's a declaration
  * @param isByname
  *   If this defintion is call by name or call by value
  */
final case class Def(rec: Bool, nme: Var, rhs: Term \/ PolyType, isByname: Bool) extends Decl with Terms {
  val body: Located = rhs.fold(identity, identity)
}

final case class TypeDef(
  kind: TypeDefKind,
  nme: TypeName,
  tparams: List[TypeName],
  body: Type,
  // maps a class to it's adt by name and maps params to adt param by position
  // for e.g. in type 'a, 'b either = Left of 'a | Right of 'b
  // Right will have an adtData = S((TypeName("either"), List(1)))
  // indicating that it's adt is either and it's param is the 1th param of either
  adtData: Opt[(TypeName, Ls[Int])] = N
) extends Decl

sealed abstract class TypeDefKind(val str: Str)
case object Cls extends TypeDefKind("class")
case object Als extends TypeDefKind("type alias")

sealed abstract class Term                                           extends Terms with TermImpl
final case class Lam(lhs: Term, rhs: Term)                           extends Term
final case class App(lhs: Term, rhs: Term)                           extends Term
final case class Tup(fields: Ls[Term]) extends Term
final case class Let(isRec: Bool, name: Var, rhs: Term, body: Term)  extends Term
/** A block of statements that are parsed and type checked together */
final case class Blk(stmts: Ls[Statement])                           extends Term with BlkImpl
/** A term is optionally ascribed with a type as in: term: ty */
final case class Asc(trm: Term, ty: Type)                            extends Term
final case class If(lhs: Term, rhs: Ls[IfBody])                    extends Term

sealed abstract class IfBody extends IfBodyImpl
final case class IfThen(expr: Term, rhs: Term) extends IfBody
final case class IfElse(expr: Term) extends IfBody

sealed abstract class SimpleTerm extends Term with SimpleTermImpl
sealed abstract class Lit                                            extends SimpleTerm
final case class Var(name: Str)                                      extends SimpleTerm with VarImpl
final case class IntLit(value: BigInt)            extends Lit
final case class DecLit(value: BigDecimal)        extends Lit
final case class StrLit(value: Str)               extends Lit
final case class UnitLit(undefinedOrNull: Bool)   extends Lit

////////////////////////////////////////////////
// Types
////////////////////////////////////////////////

sealed abstract class Type extends TypeImpl
sealed abstract class Composed(val pol: Bool) extends Type with ComposedImpl
final case class Union(lhs: Type, rhs: Type)             extends Composed(true)
final case class Inter(lhs: Type, rhs: Type)             extends Composed(false)
final case class Function(lhs: Type, rhs: Type)          extends Type
final case class Tuple(fields: Ls[Type])    extends Type
final case class Recursive(uv: TypeVar, body: Type)      extends Type
final case class AppliedType(base: TypeName, targs: List[Type]) extends Type
final case class Unified(base: Type, where: Ls[TypeVar -> Ls[Type]]) extends Type
case object Top                                          extends Type
case object Bot                                          extends Type
/** Reference to an existing type with the given name. */
final case class TypeName(name: Str)                     extends Type with TypeNameImpl
final case class TypeTag (name: Str)                     extends Type

final case class TypeVar(val identifier: Int \/ Str, nameHint: Opt[Str]) extends Type with TypeVarImpl {
  require(nameHint.isEmpty || identifier.isLeft)
  // ^ The better data structure to represent this would be an EitherOrBoth
  override def toString: Str = identifier.fold("α" + _, identity)
}

final case class PolyType(targs: Ls[TypeName], body: Type) extends PolyTypeImpl
