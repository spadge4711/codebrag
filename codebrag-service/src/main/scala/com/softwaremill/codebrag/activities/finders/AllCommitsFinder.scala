package com.softwaremill.codebrag.activities.finders

import org.bson.types.ObjectId
import com.softwaremill.codebrag.service.commits.branches.{UserReviewedCommitsCache, RepositoryCache}
import com.softwaremill.codebrag.common.paging.PagingCriteria
import com.softwaremill.codebrag.dao.commitinfo.CommitInfoDAO
import com.softwaremill.codebrag.dao.finders.views.CommitListView
import com.softwaremill.codebrag.dao.user.UserDAO
import com.typesafe.scalalogging.slf4j.Logging
import com.softwaremill.codebrag.domain.PartialCommitInfo
import CommitToViewImplicits._

class AllCommitsFinder(
  repoCache: RepositoryCache,
  val reviewedCommitsCache: UserReviewedCommitsCache,
  commitsInfoDao: CommitInfoDAO,
  val userDAO: UserDAO) extends Logging with UserDataEnhancer with CommitReviewedByUserMarker {

  def find(userId: ObjectId, branchName: String, pagingCriteria: PagingCriteria[String]): CommitListView = {
    val branchCommits = repoCache.getBranchCommits(branchName).map(_.sha)
    val page = pagingCriteria.extractPageFrom(branchCommits)
    val commits = commitsInfoDao.findByShaList(page.items)
    enhanceWithUserData(CommitListView(markAsReviewed(commits, userId), page.beforeCount, page.afterCount))
  }

  // TODO: change to Option
  def find(commitId: ObjectId, userId: ObjectId) = {
    commitsInfoDao.findByCommitId(commitId) match {
      case Some(commit) => Right(markAsReviewed(enhanceWithUserData(PartialCommitInfo(commit)), userId))
      case None => Left("Commit not found")
    }
  }

}