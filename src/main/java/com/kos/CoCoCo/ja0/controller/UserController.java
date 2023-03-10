package com.kos.CoCoCo.ja0.controller;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.kos.CoCoCo.ja0.awsS3.AwsS3;
import com.kos.CoCoCo.ja0.repository.BoardCategoryRepositoryH;
import com.kos.CoCoCo.ja0.repository.BoardRepositoryH;
import com.kos.CoCoCo.ja0.repository.KakaoUserRepository;
import com.kos.CoCoCo.ja0.repository.MessageRepositoryH;
import com.kos.CoCoCo.ja0.repository.NoticeRepositoryH;
import com.kos.CoCoCo.ja0.repository.ReplyRepositoryH;
import com.kos.CoCoCo.ja0.repository.TeamRepository;
import com.kos.CoCoCo.ja0.repository.TeamUserRepositoryH;
import com.kos.CoCoCo.ja0.repository.UserRepositoryH;
import com.kos.CoCoCo.ja0.repository.WorkManagerRepositoryH;
import com.kos.CoCoCo.ja0.repository.WorkRepositoryH;
import com.kos.CoCoCo.vo.TeamUserVO;
import com.kos.CoCoCo.vo.UserVO;
import com.kos.CoCoCo.vo.WorkManagerMultikey;
import com.kos.CoCoCo.vo.WorkManagerVO;

@Controller
public class UserController {

	@Autowired
	TeamUserRepositoryH tuRepo;
	
	@Autowired
	TeamRepository tRepo;
	
	@Autowired
	UserRepositoryH uRepo;
	
	@Autowired
	BoardCategoryRepositoryH bcRepo;
	
	@Autowired
	BoardRepositoryH bRepo;
	
	@Autowired
	NoticeRepositoryH nRepo;
	
	@Autowired
	ReplyRepositoryH rRepo;
	
	@Autowired
	WorkManagerRepositoryH wmRepo;
	
	@Autowired
	WorkRepositoryH wRepo;
	
	@Autowired
	MessageRepositoryH mRepo;
	
	@Autowired
	KakaoUserRepository kRepo;
	
	@Autowired
	AwsS3 awsS3;
	
	@GetMapping("/updateStatus/{str}/{status}")
	public String updateStatus(@PathVariable String str, @PathVariable String status, HttpSession session) {
		UserVO user = (UserVO) session.getAttribute("user");
		
		uRepo.findById(user.getUserId()).ifPresent(i->{
			i.setStatus(status);
			uRepo.save(i);
			session.setAttribute("user", i);
		});
		
		if(str.equals("t")) return "redirect:/main";
		return "redirect:/CoCoCo";
	}
	
	@PostMapping("/user/modify")
	public String modifyMyProfile(UserVO user, String fileName, String from, MultipartFile newPhoto, HttpSession session) {
		uRepo.findById(user.getUserId()).ifPresent(i->{
			if(!newPhoto.isEmpty() && newPhoto.getOriginalFilename().equals(fileName)) {
				//????????? ??????
				try {
					awsS3.delete(i.getImage()); //s3????????? ??????
					
					String img = awsS3.upload(newPhoto, "uploads/userImages/");
					i.setImage(img);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			} else if(fileName.trim().equals("")) {
				//????????? ??????
				awsS3.delete(i.getImage()); //s3????????? ??????
				i.setImage(null);
			}
			
			//?????? ????????? ?????? ?????????
			if(!user.getName().equals(i.getName())) {
				mRepo.findByWriter(i.getUserId()).forEach(w->{
					w.setKoreanName(user.getName());
					mRepo.save(w);
				});
			}
			
			i.setCompany(user.getCompany());
			i.setName(user.getName());
			
			uRepo.save(i);
			
			session.setAttribute("user", i);
		});
		
		if(from.equals("t")) return "redirect:/main";
		return "redirect:/CoCoCo";
	}
	
	@Transactional
	@GetMapping("/user/delete")
	public String deleteUser(HttpSession session, RedirectAttributes attr) {
		Optional<UserVO> none = uRepo.findById("XXXXX");
		if(!none.isPresent()) { //????????? ?????????
			UserVO newUser = UserVO.builder().userId("XXXXX").name("????????????").build();
			uRepo.save(newUser);
		}
		
		UserVO user = (UserVO) session.getAttribute("user");
		if(!adminCounter(user.getUserId())) {
			attr.addFlashAttribute("msg", "????????? ????????? ?????? ?????? ??? ????????? ???????????????!");
			return "redirect:/main";
		}
		
		changeAll(user.getUserId()); 
		
		tuRepo.deleteByUserId(user.getUserId());
		
		awsS3.delete(user.getImage()); //???????????? ????????? ??????
		uRepo.deleteById(user.getUserId());
		kRepo.deleteByEmail(user.getUserId());
		
		return "redirect:/logout";
	}

	private boolean adminCounter(String userId) {
		List<TeamUserVO> adminTeam = tuRepo.findAdminByUserId(userId); 
		
		if(!adminTeam.isEmpty()) {
			for(TeamUserVO t:adminTeam) {
				List<TeamUserVO> teamAdmin = tuRepo.findAdminByTeamId(t.getTeamUserId().getTeam().getTeamId());
				if(teamAdmin.size() == 1) {
					Long tid = teamAdmin.get(0).getTeamUserId().getTeam().getTeamId();
					if(tuRepo.findAdminByTeamId(tid).size() != 1) return false; //???????????? ????????? ????????? ?????? ??????
					
					tuRepo.delete(teamAdmin.get(0)); //???????????? ????????? ????????? ????????? ??????????????? 
					tRepo.deleteById(tid);
				}
				
				for(TeamUserVO a:teamAdmin) {
					tRepo.findById(a.getTeamUserId().getTeam().getTeamId()).ifPresent(i->{
						String creator = i.getUser().getUserId();
						if(creator.equals(userId)) { //?????? ?????? ????????????????????? ????????? ?????? ??????
							UserVO userNone = uRepo.findById("XXXXX").get();
							i.setUser(userNone);
							tRepo.save(i);
						}
					});
				}
			}
		}
		return true;
	}
	
	private void changeAll(String userId) {
		UserVO userNone = uRepo.findById("XXXXX").get();
		
		//??????
		nRepo.findByUserId(userId).forEach(i->{
			i.setUser(userNone);
			nRepo.save(i);
		});
		
		//?????????
		bRepo.findByUserId(userId).forEach(i->{
			i.setUser(userNone);
			bRepo.save(i);
		});
		
		//????????? ??????
		rRepo.findByUserId(userId).forEach(i->{
			i.setUser(userNone);
			rRepo.save(i);
		});
		
		//?????? ?????????
		List<WorkManagerVO> wmList =  wmRepo.findByUserId(userId);
		for(WorkManagerVO wm:wmList) {
			WorkManagerMultikey wmId = new WorkManagerMultikey(wm.getWorkManagerId().getWork(), userNone);
			
			wmRepo.delete(wm); //??????????????? ?????????
			
			WorkManagerVO newWork = new WorkManagerVO(wmId);
			wmRepo.save(newWork); //????????? ?????? ??????
		}
		
		//??????
		mRepo.findByUserId(userId).forEach(i->{
			i.setWriter(userNone.getUserId());
			i.setKoreanName(userNone.getName());
			mRepo.save(i);
		});
	}
}
