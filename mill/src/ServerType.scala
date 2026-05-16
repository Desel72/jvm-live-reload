package me.seroperson.reload.live.mill

sealed trait ServerType

case object HttpServerType extends ServerType

case object GrpcServerType extends ServerType
