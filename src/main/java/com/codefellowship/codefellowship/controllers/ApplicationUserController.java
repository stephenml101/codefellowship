package com.codefellowship.codefellowship.controllers;

import com.codefellowship.codefellowship.models.ApplicationUser;
import com.codefellowship.codefellowship.repos.ApplicationUserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import java.security.Principal;
import java.time.LocalDate;

@Controller
public class ApplicationUserController {
    @Autowired
    ApplicationUserRepository applicationUserRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    private HttpServletRequest request;

    @GetMapping("/")
    public String getHomePage(Model m, Principal p) {
        if (p != null) {
            String username = p.getName();
            ApplicationUser user = applicationUserRepository.findByUsername(username);

            m.addAttribute("username", username);
        }
        return "index.html";
    }

    @GetMapping("/login")
    public String getLoginPage(Principal p) {
        if (p != null) {
            return "redirect:/";
        }
        return "/login.html";
    }

    @GetMapping("/signup")
    public String getSignUpPage() {
        return "signup.html";
    }

    @PostMapping("/signup")
    public RedirectView createUser(String username, String password, String firstName, String lastName, LocalDate dateOfBirth, String bio) {
        ApplicationUser newUser = new ApplicationUser(username, passwordEncoder.encode(password), firstName, lastName, dateOfBirth, bio);
        applicationUserRepository.save(newUser);
        authWithHttpServletRequest(username, password);
        return new RedirectView("/");
//        newUser.setDateCreated(LocalDate.now());
    }

    public void authWithHttpServletRequest(String username, String password) {
        try {
            request.login(username, password);
        } catch (ServletException e) {
            System.out.println("Error while logging in!");
            e.printStackTrace();
        }

        @GetMapping("/users/{id}")
        public String getUserInfoPage(Model m, Principal p, @PathVariable long id) {
            if(p != null) { //not strictly needed if WebSecurityConfig is set up properly
                String username = p.getName();
                ApplicationUser browsingUser = applicationUserRepository.findByUsername(username);
                m.addAttribute("username", browsingUser.getUsername());
            }

            ApplicationUser profileUser = applicationUserRepository.findById(id).orElseThrow();
            m.addAttribute("profileUsername", profileUser.getUsername());
            m.addAttribute("profileId", profileUser.getId());
            m.addAttribute("profileDateCreated", profileUser.getDateCreated());

            return "ApplicationUser.html";
        }

        @PutMapping("/users/{id}")
        public RedirectView updateUserInfo(Model m, Principal p, @PathVariable Long id, String profileUsername,
                RedirectAttributes redir) {
            ApplicationUser userToBeEdited = applicationUserRepository.findById(id).orElseThrow();
            if(p != null && p.getName().equals(userToBeEdited.getUsername())) {
                userToBeEdited.setUsername(profileUsername);
                applicationUserRepository.save(userToBeEdited);

                // include lines below if your principal is not updating
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userToBeEdited, userToBeEdited.getPassword(),
                        userToBeEdited.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                redir.addFlashAttribute("errorMessage", "Cannont edit another user's page!");
            }

            return new RedirectView("/users/"+id);
        }

        //METHOD FOR DELETING A USER
        @DeleteMapping("/users/{id}")
        public RedirectView deleteUser(@PathVariable Long id, Principal p, RedirectAttributes redir) {

            ApplicationUser userToDelete = applicationUserRepository.findById(id).orElseThrow();
            if(p != null && p.getName().equals(userToDelete.getUsername())) {
                applicationUserRepository.deleteById(id);
                p = null;
            } else {
                redir.addFlashAttribute("errorMessage", "Cannot delete another user's account!");
                return new RedirectView("/users/"+id);
            }

            return new RedirectView("/");
        }
    }
}
