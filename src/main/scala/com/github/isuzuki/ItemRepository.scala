package com.github.isuzuki

import doobie.free.connection.ConnectionIO

trait ItemRepository {
  def findById(id: String): ConnectionIO[Option[Item]]
  def update(item: Item): ConnectionIO[Int]
}
