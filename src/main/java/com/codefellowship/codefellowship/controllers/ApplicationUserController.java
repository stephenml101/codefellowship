package com.codefellowship.codefellowship.controllers;

import com.codefellowship.codefellowship.models.ApplicationUser;
import com.codefellowship.codefellowship.models.Post;
import com.codefellowship.codefellowship.repos.ApplicationUserRepository;
import com.codefellowship.codefellowship.repos.PostRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Date;

@Controller
public class ApplicationUserController {
    @Autowired
    ApplicationUserRepository applicationUserRepository;
    @Autowired
    PostRepository postRepository;
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
        return "login.html";
    }

    @GetMapping("/signup")
    public String getSignUpPage() {
        return "signup.html";
    }

    @PostMapping("/signup")
    public RedirectView createUser(String username, String password, String firstName, String lastName, LocalDate dateOfBirth, String bio, RedirectAttributes redir) {
        // Check if the username already exists
        if (applicationUserRepository.findByUsername(username) != null) {
            redir.addFlashAttribute("errorMessage", "That username already exists!");
            return new RedirectView("/signup?error");
        }

        ApplicationUser newUser = new ApplicationUser(username, passwordEncoder.encode(password), firstName, lastName, dateOfBirth, bio);
        applicationUserRepository.save(newUser);
        authWithHttpServletRequest(username, password);
        return new RedirectView("/");
    }

    public void authWithHttpServletRequest(String username, String password) {
        try {
            request.login(username, password);
        } catch (ServletException e) {
            System.out.println("Error while logging in!");
            e.printStackTrace();
        }
    }

    @GetMapping("/myprofile")
    public String getMyProfile(Model m, Principal p) {
        if(p != null) {
            ApplicationUser user = applicationUserRepository.findByUsername(p.getName());
            m.addAttribute("user", user);
            m.addAttribute("username", user.getUsername());
            return "myprofile";
        }
        return "login";
    }

    @PutMapping("/myprofile")
    public RedirectView editProfile(Principal p, String username, String firstName, String lastName, LocalDate dateOfBirth, String bio, Long id, RedirectAttributes redir) {
        ApplicationUser user = applicationUserRepository.findById(id).orElseThrow();
        if(p != null) { //not strictly needed if WebSecurityConfig is set up properly
            user.setUsername(username);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setDateOfBirth(dateOfBirth);
            user.setBio(bio);
            applicationUserRepository.save(user);

            // include lines below if your principal is not updating
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, user.getPassword(),
                    user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            redir.addFlashAttribute("errorMessage", "You are not permitted to edit this profile!");
        }

        return new RedirectView("/myprofile");
    }

    @PostMapping("/createPost")
    public RedirectView createPost(Principal p, String body,long id, RedirectAttributes redir) {
        ApplicationUser user = applicationUserRepository.findById(id).orElseThrow();
        if(p != null) {
            Date date = new Date();
            Post post = new Post(body, date);
            user.addPost(post);
            postRepository.save(post);
            applicationUserRepository.save(user);
        } else {
            redir.addFlashAttribute("errorMessage", "You are not permitted to add posts to this profile!");
        }
        return new RedirectView("/myprofile");
    }

    @GetMapping("/user/{id}")
    public String getUserInfoPage(Model m, Principal p, @PathVariable long id, RedirectAttributes redir) {
        if(p != null) {
            ApplicationUser user = applicationUserRepository.findById(id).orElseThrow();
            m.addAttribute("user", user);
            return "profile";
        } else {
            redir.addFlashAttribute("errorMessage", "You must be logged in to view this profile!");
            return "redirect:/login";
        }
    }

}