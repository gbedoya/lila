package lila.oauth

import org.joda.time.DateTime
import reactivemongo.api.bson._

import lila.common.{ Bearer, SecureRandom }
import lila.user.User

case class AccessToken(
    id: Bearer,
    publicId: BSONObjectID,
    userId: User.ID,
    createdAt: Option[DateTime] = None, // for personal access tokens
    description: Option[String] = None, // for personal access tokens
    usedAt: Option[DateTime] = None,
    scopes: List[OAuthScope],
    clientOrigin: Option[String],
    expires: Option[DateTime]
) {
  def isBrandNew = createdAt.exists(DateTime.now.minusSeconds(5).isBefore)
}

object AccessToken {

  case class ForAuth(userId: User.ID, scopes: List[OAuthScope])

  object BSONFields {
    val id           = "access_token_id"
    val publicId     = "_id"
    val userId       = "user_id"
    val createdAt    = "create_date"
    val description  = "description"
    val usedAt       = "used_at"
    val scopes       = "scopes"
    val clientOrigin = "clientOrigin"
    val expires      = "expires"
  }

  import lila.db.BSON
  import lila.db.dsl._
  import BSON.BSONJodaDateTimeHandler
  import OAuthScope.scopeHandler

  private[oauth] val forAuthProjection = $doc(
    BSONFields.userId -> true,
    BSONFields.scopes -> true
  )

  implicit private[oauth] val accessTokenIdHandler = stringAnyValHandler[Bearer](_.secret, Bearer.apply)

  implicit val ForAuthBSONReader = new BSONDocumentReader[ForAuth] {
    def readDocument(doc: BSONDocument) =
      for {
        userId <- doc.getAsTry[User.ID](BSONFields.userId)
        scopes <- doc.getAsTry[List[OAuthScope]](BSONFields.scopes)
      } yield ForAuth(userId, scopes)
  }

  implicit val AccessTokenBSONHandler = new BSON[AccessToken] {

    import BSONFields._

    def reads(r: BSON.Reader): AccessToken =
      AccessToken(
        id = r.get[Bearer](id),
        publicId = r.get[BSONObjectID](publicId),
        userId = r str userId,
        createdAt = r.getO[DateTime](createdAt),
        description = r strO description,
        usedAt = r.getO[DateTime](usedAt),
        scopes = r.get[List[OAuthScope]](scopes),
        clientOrigin = r strO clientOrigin,
        expires = r.getO[DateTime](expires)
      )

    def writes(w: BSON.Writer, o: AccessToken) =
      $doc(
        id           -> o.id,
        publicId     -> o.publicId,
        userId       -> o.userId,
        createdAt    -> o.createdAt,
        description  -> o.description,
        usedAt       -> o.usedAt,
        scopes       -> o.scopes,
        clientOrigin -> o.clientOrigin,
        expires      -> o.expires
      )
  }
}
