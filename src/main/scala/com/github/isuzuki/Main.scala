package com.github.isuzuki

import cats.data.{EitherT, OptionT}
import cats.effect.ExitCase.Error
import cats.effect.{ExitCase, IO}
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor

import scala.concurrent.ExecutionContext

object Main extends App {
  lazy val repository = new ItemRepository {
    override def findById(id: String): ConnectionIO[Option[Item]] =
      sql"select id, name from item where id = $id".query[Item].option

    override def update(item: Item): ConnectionIO[Int] =
      sql"update item set name = ${item.name} where id = ${item.id}".update.run
  }

  implicit val cs = IO.contextShift(ExecutionContext.global)
  val xa = Transactor
    .fromDriverManager[IO]("org.postgresql.Driver", "jdbc:postgresql://localhost:5432/doobie", "doobie", "doobie")

  val io = for {
    item <- OptionT(repository.findById("hoge"))
    result <- OptionT.liftF(repository.update(item.copy(name = "bar")))
  } yield result
  io.value.transact(xa).unsafeRunSync().foreach(println)

  val io2 = for {
    item <- EitherT.fromOptionF(repository.findById("hoge"), Error("not found."))
    result <- EitherT.liftF[ConnectionIO, ExitCase[String], Int](repository.update(item.copy(name = "baz")))
  } yield result
  io2.value.transact(xa).unsafeRunSync().foreach(println)
}
