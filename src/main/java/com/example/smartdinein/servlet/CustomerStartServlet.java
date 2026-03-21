package com.example.smartdinein.servlet;

import com.example.smartdinein.dao.OtpDAO;
import com.example.smartdinein.util.EmailService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Random;

@WebServlet("/start")
public class CustomerStartServlet extends HttpServlet {
    private final OtpDAO otpDAO = new OtpDAO();
    private final EmailService emailService = new EmailService();
    private final Random random = new Random();

    private String generateOtp() {
        int n = 100000 + random.nextInt(900000);
        return String.valueOf(n);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/customer/start.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String name = req.getParameter("name");
        String email = req.getParameter("email");

        if (name == null) name = "";
        if (email == null) email = "";
        name = name.trim();
        email = email.trim().toLowerCase();

        if (name.isBlank() || email.isBlank() || !email.contains("@")) {
            req.setAttribute("error", "Please enter a valid name and email.");
            req.getRequestDispatcher("/customer/start.jsp").forward(req, resp);
            return;
        }

        String otp = generateOtp();
        Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + 5L * 60L * 1000L);
        otpDAO.createOtp(email, otp, expiresAt);
        emailService.sendOtpEmail(email, otp);

        HttpSession session = req.getSession(true);
        session.setAttribute("pendingName", name);
        session.setAttribute("pendingEmail", email);

        resp.sendRedirect(req.getContextPath() + "/otp");
    }
}

