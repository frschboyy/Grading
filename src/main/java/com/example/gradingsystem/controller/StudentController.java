package com.example.gradingsystem.controller;

import com.example.gradingsystem.model.Student;
import com.example.gradingsystem.service.StudentService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class StudentController {
    @Autowired
    private StudentService studentService;

    @GetMapping("/signup")
    public String showSignupPage(Model model) {
        model.addAttribute("student", new Student());
        return "signup";
    }

    @PostMapping("/signup")
    public String handleSignup(@ModelAttribute("student") Student student) {
        studentService.saveStudent(student);
        return "redirect:/";
    }

    @GetMapping("/")
    public String showLoginPage(HttpSession session, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0
        response.setDateHeader("Expires", 0); // Proxies
        
        Student loggedInStudent = (Student) session.getAttribute("loggedInStudent");
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");

        if (isAdmin != null && isAdmin) {
            // If session exists, redirect to the dashboard
            return "redirect:/add-Assignment";
        }
        
        if (loggedInStudent != null) {
            // If session exists, redirect to the dashboard
            return "redirect:/dashboard";
        }
        
        //  Else show login page
        return "login";
    }

    @PostMapping("/")
    public String handleLogin(@RequestParam String username, @RequestParam String password, 
                              HttpSession session, Model model) {
        
         // Check for admin credentials
        if ("admin".equals(username) && "admin".equals(password)) {
            session.setAttribute("isAdmin", true); // Add admin session attribute
            return "redirect:/add-Assignment"; // Redirect to Add Assignment page
        }
        
        Student student = studentService.getStudentByName(username);

        if (student != null && student.getPassword().equals(password)) {
            // Store the student in the session
            session.setAttribute("loggedInStudent", student);
            return "redirect:/dashboard";
        }

        model.addAttribute("error", "Invalid username or password");
        return "login";
    }

    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        Student loggedInStudent = (Student) session.getAttribute("loggedInStudent");

        if (loggedInStudent == null) {
            return "redirect:/"; // Redirect to login if not authenticated
        }

        // Pass the username to the dashboard
        model.addAttribute("username", loggedInStudent.getUsername());
        model.addAttribute("id", loggedInStudent.getId());
        return "dashboard";
    }
    
    @GetMapping("/add-Assignment")
    public String showAddAssignmentPage(HttpSession session) {
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");

        if (isAdmin != null && isAdmin) {
            return "addAssignment"; // Return the add-assignment view
        }

        return "redirect:/"; // Redirect to login if not authenticated as admin
    }

    @GetMapping("/logout")
    public String handleLogout(HttpSession session) {
        session.invalidate(); // Clear the session
        return "redirect:/";
    }
    
    @GetMapping("/submit-page")
    public String getSubmitAssignmentPage(Model model, HttpSession session) {
        // Add assignment details to the model
        model.addAttribute("id", (Long) session.getAttribute("id"));
        model.addAttribute("title", (String) session.getAttribute("title"));
        model.addAttribute("description", (String) session.getAttribute("description"));
        model.addAttribute("dueDate", session.getAttribute("dueDate"));

        // Return the view
        return "submitAssignmentPage";
    }
}