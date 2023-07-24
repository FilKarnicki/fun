package eu.karnicki.fun

import scala.annotation.tailrec

object SomeSorting:
  def sortList(list: List[Int]): List[Int] =
    // insertion sort w/tail recursion
    @tailrec
    def insert(preList: List[Int])(number: Int, sortedList: List[Int]) : List[Int] =
      if sortedList.isEmpty || number <= sortedList.head then preList ++ (number +: sortedList)
      else insert(preList :+ sortedList.head)(number, sortedList.tail)

    if list.isEmpty || list.tail.isEmpty then list
    else insert(List.empty)(list.head, sortList(list.tail))

  def main(args: Array[String]): Unit =
    val sortedList = sortList(List(3,2,4,1,6))
    println(sortedList)
