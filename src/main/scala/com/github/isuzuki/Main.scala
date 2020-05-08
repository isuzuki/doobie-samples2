package com.github.isuzuki

import cats.data.{EitherT, OptionT}
import cats.effect.ExitCase.Error
import cats.effect.{Blocker, ContextShift, ExitCase, IO, Resource}
import doobie.free.connection.ConnectionIO
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.util.ExecutionContexts

import scala.concurrent.ExecutionContext

object Main extends App {
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  val transactor: Resource[IO, HikariTransactor[IO]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32) // our connect EC
      be <- Blocker[IO] // our blocking EC
      xa <- HikariTransactor.newHikariTransactor[IO](
        "org.postgresql.Driver",
        "jdbc:postgresql://localhost:5432/doobie",
        "doobie",
        "doobie",
        ce, // await connection here
        be // execute JDBC operations here
      )
    } yield xa

  lazy val repository = new PostgreSQLItemRepository

  val io = for {
    item <- OptionT(repository.findById("hoge"))
    result <- OptionT.liftF(repository.update(item.copy(name = "bar")))
  } yield result
  transactor.use(xa => io.value.transact(xa)).unsafeRunSync().foreach(println)

  val io2 = for {
    item <- EitherT.fromOptionF(repository.findById("hoge"), Error("not found."))
    result <- EitherT.liftF[ConnectionIO, ExitCase[String], Int](repository.update(item.copy(name = "baz")))
  } yield result
  transactor.use(xa => io2.value.transact(xa)).unsafeRunSync().foreach(println)

  val io3 = OptionT(repository.findById("fuga"))
    .getOrElseF {
      val item = Item("fuga", "qux")
      repository.insert(item).map(_ => item)
    }
  transactor.use(xa => io3.transact(xa)).unsafeRunSync()

  val io4 = repository.findById("piyo").flatMap {
    case Some(item) => doobie.FC.pure(item)
    case _ =>
      val item = Item("piyo", "quux")
      repository.insert(item).map(_ => item)
  }
  transactor.use(xa => io4.transact(xa)).unsafeRunSync()

  transactor.use(xa => repository.getAll.transact(xa)).unsafeRunSync().foreach(println)
}
