package com.kos.CoCoCo.ja0.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.kos.CoCoCo.vo.BoardVO;


public interface BoardRepositoryH extends CrudRepository<BoardVO, Long>{

	@Query(value = "select * from boards where category_team_team_id = ?1", nativeQuery = true)
	List<BoardVO> findByTeamId(Long teamId);
	
	@Query(value = "select * from boards where user_user_id = ?1", nativeQuery = true)
	List<BoardVO> findByUserId(String userId);
	
	
	@Query(value = "select * from boards where category_category_id = ?1", nativeQuery = true)
	List<BoardVO> findByCategoryId(Long categoryId);
	
	@Modifying
	@Query(value = "delete from boards where category_team_team_id = ?1", nativeQuery = true)
	void deleteByTeamId(Long teamId);
}
