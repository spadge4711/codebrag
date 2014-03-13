package com.softwaremill.codebrag.service.commits

import org.scalatest.{FlatSpec, BeforeAndAfter}
import org.scalatest.mock.MockitoSugar
import org.scalatest.matchers.ShouldMatchers
import com.softwaremill.codebrag.domain.{RepositoryStatus, MultibranchLoadCommitsResult}
import com.softwaremill.codebrag.service.events.MockEventBus
import com.softwaremill.codebrag.common.ClockSpec
import org.mockito.Mockito._
import com.softwaremill.codebrag.dao.repositorystatus.RepositoryStatusDAO
import com.softwaremill.codebrag.service.commits.branches.RepositoryCache
import com.softwaremill.codebrag.repository.Repository
import org.mockito.Matchers

class CommitImportServiceSpec extends FlatSpec with MockitoSugar with BeforeAndAfter with ShouldMatchers with MockEventBus with ClockSpec {

  var repoStatusDao: RepositoryStatusDAO = _
  var repoCache: RepositoryCache = _
  var repository: Repository = _
  var service: CommitImportService = _

  val SavedRepoState = Map.empty[String, String]
  val LoadedCommits = MultibranchLoadCommitsResult("test-repo", List.empty)

  before {
    eventBus.clear()
    repoStatusDao = mock[RepositoryStatusDAO]
    repoCache = mock[RepositoryCache]
    repository = mock[Repository]
    service = new CommitImportService(repoStatusDao, eventBus, repoCache)
  }

  it should "pull changes and load commits from repo since given (saved) state" in {
    // given
    when(repoStatusDao.loadBranchesState).thenReturn(SavedRepoState)

    // when
    service.importRepoCommits(repository)

    // then
    verify(repository).pullChanges()
    verify(repository).loadCommitsSince(SavedRepoState)
  }
  
  it should "add loaded commits to cache" in {
    // given
    when(repoStatusDao.loadBranchesState).thenReturn(SavedRepoState)
    when(repository.loadCommitsSince(SavedRepoState)).thenReturn(LoadedCommits)
    
    // when
    service.importRepoCommits(repository)
    
    // then
    verify(repoCache).addCommits(LoadedCommits)
  }

  it should "update repo status to not-ready when commits import failed" in {
    // given
    when(repository.loadCommitsSince(Matchers.any[Map[String, String]])).thenThrow(new RuntimeException("oops"))

    // when
    service.importRepoCommits(repository)

    // then
    val expectedRepoStatus = RepositoryStatus.notReady(repository.repoName, Some("oops"))
    verify(repoStatusDao).updateRepoStatus(expectedRepoStatus)
  }

}