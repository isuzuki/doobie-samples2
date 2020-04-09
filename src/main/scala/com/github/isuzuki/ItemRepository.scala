package com.github.isuzuki

import doobie.free.connection.ConnectionIO
import doobie.implicits._

trait ItemRepository {
  def getAll: ConnectionIO[Seq[Item]]
  def findById(id: String): ConnectionIO[Option[Item]]
  def update(item: Item): ConnectionIO[Int]
}

class PostgreSQLItemRepository extends ItemRepository {
  override def getAll: ConnectionIO[Seq[Item]] =
    // no implicit parameter is known issues
    // @see https://github.com/tpolecat/doobie/issues/1061
    sql"select id, name from item".query[Item].to[Seq]

  override def findById(id: String): ConnectionIO[Option[Item]] =
    sql"select id, name from item where id = $id".query[Item].option

  override def update(item: Item): ConnectionIO[Int] =
    sql"update item set name = ${item.name} where id = ${item.id}".update.run
}
