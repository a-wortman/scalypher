package com.scalypher.path

import com.scalypher.path.Path.getIdentifierOrEmptyString
import com.scalypher.ToQueryWithIdentifiers
import com.scalypher.types.ReferenceableMap
import com.scalypher.types.Referenceable
import com.scalypher.Label
import com.scalypher.Property
import com.scalypher.PropertyReference
import com.scalypher.where.Reference
import scala.language.implicitConversions

sealed trait NodeType extends ToQueryWithIdentifiers with Referenceable {
  def property(propertyReference: String): Reference =
    Reference(this, PropertyReference(propertyReference))
}

object NodeType {
  implicit def toPath(node: NodeType): Path =
    Path(node)
}

class AnyNode extends NodeType {
  def toQuery(referenceableMap: ReferenceableMap): String = {
    val identifier = getIdentifierOrEmptyString(referenceableMap, this)
    s"($identifier)"
  }
}

object AnyNode {
  def apply(): AnyNode =
    new AnyNode
}

class CypherNode(
  val labels: Seq[Label] = Seq.empty,
  val properties: Seq[Property] = Seq.empty
) extends NodeType {
  def toQuery(referenceableMap: ReferenceableMap): String = {
    val identifier = getIdentifierOrEmptyString(referenceableMap, this)
    val labelsQuery = labels map (_.toQuery) mkString ""
    val propertiesQuery =
      if (properties.isEmpty) ""
      else "{" + (properties map (_.toQuery) mkString ",") + "}"

    s"($identifier$labelsQuery$propertiesQuery)"
  }
}

object CypherNode {

  def apply(label: String): CypherNode =
    CypherNode(Label(label))

  def apply(label: Label): CypherNode =
    new CypherNode(labels = Seq(label))

  def apply(property: Property): CypherNode =
    new CypherNode(properties = Seq(property))

  def apply(label: Label, property: Property): CypherNode =
    new CypherNode(Seq(label), Seq(property))

  def apply(label: String, kv: (String, String)): CypherNode =
    new CypherNode(Seq(Label(label)), Seq(Property.apply(kv)))

  def apply(labels: Seq[String], keyValues: Seq[(String, String)]): CypherNode =
    new CypherNode(labels map Label.apply, keyValues map (kv => Property.apply(kv)))

}