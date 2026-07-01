package com.stie.controller;

import com.stie.config.AppConstants;
import com.stie.service.AuditService;
import com.stie.service.UserService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ProfileController {

	@Autowired
	private UserService userService;

	@Autowired
	private AuditService auditService;

	private String getCurrentUser() {
		return SecurityContextHolder.getContext().getAuthentication().getName();
	}

	@GetMapping("/profile")
	public String showProfile(Model model) {
		model.addAttribute("pageTitle", "User Profile");
		model.addAttribute("user", userService.findByUsername(getCurrentUser()));
		return "profile";
	}

	@RequestMapping("/downloadDocument")
	public void downloadPodocument(@RequestParam("id") String id, HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		File file = new File(id);
		InputStream is = new FileInputStream(AppConstants.FilePaths.UPLOAD_DIR + file);

		// MIME type of the file

		if (id.contains(".pdf"))
			response.setContentType("application/pdf");
		else if (id.contains(".png"))
			response.setContentType("image/png");
		else if (id.contains(".gif"))
			response.setContentType("image/gif");
		else if (id.contains(".jpg"))
			response.setContentType("image/jpg");
		else if (id.contains(".docx")) {
			response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		} else if (id.contains(".doc")) {
			response.setContentType("application/msword");
		} else
			response.setContentType("image/jpeg");

		// response.setContentType("application/pdf");
		// Response header
		response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
		// Read from the file and write into the response
		OutputStream os = response.getOutputStream();
		byte[] buffer = new byte[1024];
		int len;
		while ((len = is.read(buffer)) != -1) {
			os.write(buffer, 0, len);
		}
		os.flush();
		os.close();
		is.close();

	}

	@RequestMapping("/downloadSignedDocument")
	public void downloadSigneddocument(@RequestParam("id") String id, HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		File file = new File(id);
		InputStream is = new FileInputStream(
				AppConstants.FilePaths.SIGNED_OFFERS_SUBDIR.replace("offers/signed/", "") + file);

		// MIME type of the file

		if (id.contains(".pdf"))
			response.setContentType("application/pdf");
		else if (id.contains(".png"))
			response.setContentType("image/png");
		else if (id.contains(".gif"))
			response.setContentType("image/gif");
		else if (id.contains(".jpg"))
			response.setContentType("image/jpg");
		else if (id.contains(".docx")) {
			response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		} else if (id.contains(".doc")) {
			response.setContentType("application/msword");
		} else
			response.setContentType("image/jpeg");

		// response.setContentType("application/pdf");
		// Response header
		response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
		// Read from the file and write into the response
		OutputStream os = response.getOutputStream();
		byte[] buffer = new byte[1024];
		int len;
		while ((len = is.read(buffer)) != -1) {
			os.write(buffer, 0, len);
		}
		os.flush();
		os.close();
		is.close();

	}

	@RequestMapping(value = "/getPhoto", method = RequestMethod.GET)
	public void getUserImage(@RequestParam("imageName") String imageName, Model model, HttpServletRequest req,
			HttpServletResponse rep) {

		try {
			System.out.println("i m in getsig-----------------------------------------------");
			// MechanicalEquipment e = mechanicalEquipmentService.find(id);
			System.out.println(
					"imgeName=---------------------------------------------------------------------------" + imageName);
			InputStream is = new FileInputStream(AppConstants.FilePaths.UPLOAD_DIR + imageName);

			byte[] bytes = IOUtils.toByteArray(is);
			if (imageName.contains(".pdf"))
				rep.setContentType("application/pdf");
			else if (imageName.contains(".png"))
				rep.setContentType("image/png");
			else if (imageName.contains(".gif"))
				rep.setContentType("image/gif");
			else if (imageName.contains(".jpg"))
				rep.setContentType("image/jpg");
			else
				rep.setContentType("image/jpeg");
			OutputStream os = rep.getOutputStream();
			os.write(bytes);
			os.close();
			is.close();

		} catch (Exception e) {// e.printStackTrace();
			System.out.println("Image " + imageName + " not present");
		}
	}

}
