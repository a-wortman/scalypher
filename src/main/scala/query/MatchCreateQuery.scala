package com.originate.scalypher

import com.originate.scalypher.action.ActionReference
import com.originate.scalypher.action.ReturnAction
import com.originate.scalypher.action.ReturnAll
import com.originate.scalypher.action.ReturnDistinct
import com.originate.scalypher.action.ReturnReference
import com.originate.scalypher.path.AnyNode
import com.originate.scalypher.path.AnyRelationship
import com.originate.scalypher.path.Node
import com.originate.scalypher.path.Path
import com.originate.scalypher.path.Relationship
import com.originate.scalypher.types._
import com.originate.scalypher.where.Reference

trait MatchCreateQuery extends Query {

  def returnAction: Option[ReturnAction]

  def returns(reference: ActionReference, rest: ActionReference*): MatchCreateQuery =
    withReturnAction(ReturnReference(reference, rest: _*))

  def returnDistinct(reference: ActionReference, rest: ActionReference*): MatchCreateQuery =
    withReturnAction(ReturnDistinct(reference, rest: _*))

  def returnAll: MatchCreateQuery =
    withReturnAction(ReturnAll)

  def getReturnColumns: Set[String] =
    returnAction match {
      case Some(action) => matchActionToReturnColumns(action)
      case _ => Set.empty
    }

  protected def withReturnAction(action: ReturnAction): MatchCreateQuery

  protected def forcedCreateReferenceables: Set[Referenceable]

  protected def modifiedReferenceableMap: ReferenceableMap = {
    val forcedMap = referenceableMap filterKeys (forcedCreateReferenceables contains _)
    createMap ++ forcedMap
  }

  protected def cleanedCreatePath: Path

  protected def createMap: ReferenceableMap

  protected def cleanPathAndExtractMap(path: Path, matchPaths: Seq[Path]): (Path, ReferenceableMap) = {
    val overlapReferenceables = matchPaths flatMap (_.referenceables) intersect path.referenceables.toSeq
    val relevantMap = referenceableMap filterKeys { key =>
      overlapReferenceables contains key
    }

    val pathTransform = relevantMap.foldLeft(PathTranform(path)) { case (acc @ PathTranform(path, map), (referenceable, identifier)) =>
      referenceable match {
        case node: Node =>
          val newNode = AnyNode()
          PathTranform(path.replaceNode(node, newNode), map - referenceable + (newNode -> identifier))
        case relationship: Relationship =>
          val newRelationship = AnyRelationship()
          PathTranform(path.replaceRelationship(relationship, newRelationship), map - referenceable + (newRelationship -> identifier))
        case _ =>
          acc
      }
    }

    (pathTransform.path, pathTransform.map)
  }

  private case class PathTranform(path: Path, map: ReferenceableMap = Map[Referenceable, String]())

}
