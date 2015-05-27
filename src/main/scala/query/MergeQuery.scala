package com.originate.scalypher

import action.ReturnAction
import path.Path
import types._

case class MergeQuery(
  mergePath: Path,
  matchPaths: Seq[Path] = Seq.empty,
  createProperties: Seq[SetProperty] = Seq.empty,
  mergeProperties: Seq[SetProperty] = Seq.empty,
  returnAction: Option[ReturnAction] = None
) extends MatchCreateQuery {

  def toQuery: String = {
    val matchString = ifNonEmpty(matchPaths) { paths =>
      stringListWithPrefix("MATCH", matchPaths map (_.toQuery(referenceableMap)))
    }
    val mergeString = Some(s"MERGE " + cleanedCreatePath.toQuery(modifiedReferenceableMap))
    val onCreateString = ifNonEmpty(createProperties) { properties =>
      stringListWithPrefix("ON CREATE SET", properties map (_.toQuery(referenceableMap)))
    }
    val onMergeString = ifNonEmpty(mergeProperties) { properties =>
      stringListWithPrefix("ON MERGE SET", properties map (_.toQuery(referenceableMap)))
    }
    val returnString = returnAction map (_.toQuery(referenceableMap))

    buildQuery(
      matchString,
      mergeString,
      onCreateString,
      onMergeString,
      returnString
    )
  }

  protected def withReturnAction(action: ReturnAction): MergeQuery =
    copy(returnAction = Some(action))

  private val onCreateOrMergeReferenceables =
    createProperties.flatMap(_.getReferenceable).toSet ++
      mergeProperties.flatMap(_.getReferenceable).toSet

  protected val referenceableMap: ReferenceableMap =
    referenceableMapWithPathWhereAndAction(
      matchPaths,
      None,
      returnAction,
      mergePath.referenceables - mergePath ++ onCreateOrMergeReferenceables
    )

  protected val (cleanedCreatePath, createMap) =
    cleanPathAndExtractMap(mergePath, matchPaths)

}
