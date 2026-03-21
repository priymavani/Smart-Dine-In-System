package com.example.smartdinein.servlet;

import com.example.smartdinein.dao.MenuItemDAO;
import com.example.smartdinein.dao.OfferDAO;
import com.example.smartdinein.dao.OrderDAO;
import com.example.smartdinein.model.MenuItem;
import com.example.smartdinein.model.Offer;
import com.example.smartdinein.model.OrderSummary;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@WebServlet("/customer/home")
public class CustomerHomeServlet extends HttpServlet {
    private final MenuItemDAO menuItemDAO = new MenuItemDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final OfferDAO offerDAO = new OfferDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("dineInSessionId") == null) {
            // Not in an active table session yet
            resp.sendRedirect(req.getContextPath() + "/table");
            return;
        }

        if (session.getAttribute("customerId") == null) {
            resp.sendRedirect(req.getContextPath() + "/start");
            return;
        }

        int sessionId = (Integer) session.getAttribute("dineInSessionId");
        int customerId = (Integer) session.getAttribute("customerId");

        com.example.smartdinein.dao.DineInSessionDAO dineInSessionDAO = new com.example.smartdinein.dao.DineInSessionDAO();
        com.example.smartdinein.model.DineInSession currentSession = dineInSessionDAO.getSessionById(sessionId, customerId);

        if (currentSession != null) {
            if ("PAID".equals(currentSession.getStatus())) {
                session.removeAttribute("dineInSessionId");
                session.removeAttribute("tableNumber");
                resp.sendRedirect(req.getContextPath() + "/customer/history");
                return;
            } else if ("BILL_REQUESTED".equals(currentSession.getStatus())) {
                resp.sendRedirect(req.getContextPath() + "/customer/request-bill");
                return;
            }
        }

        List<MenuItem> menuItems = menuItemDAO.findAll();
        BigDecimal subtotal = orderDAO.getSessionSubtotal(sessionId);
        List<OrderSummary> recentOrders = orderDAO.getOrdersBySession(sessionId);
        
        List<Offer> activeOffers = new java.util.ArrayList<>();
        try {
            activeOffers = offerDAO.findAllActiveForCustomer(customerId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Build distinct ordered categories for filter tabs
        java.util.LinkedHashSet<String> catSet = new java.util.LinkedHashSet<>();
        for (MenuItem item : menuItems) {
            if (item.getCategory() != null && !item.getCategory().isBlank()) {
                catSet.add(item.getCategory());
            }
        }

        req.setAttribute("menuItems", menuItems);
        req.setAttribute("sessionSubtotal", subtotal);
        req.setAttribute("recentOrders", recentOrders);
        req.setAttribute("categories", new java.util.ArrayList<>(catSet));
        req.setAttribute("activeOffers", activeOffers);
        
        String orderSuccess = req.getParameter("orderSuccess");
        if ("true".equals(orderSuccess)) {
            req.setAttribute("successMessage", "Order placed successfully!");
        }

        String error = req.getParameter("error");
        if (error != null && !error.isBlank()) {
            req.setAttribute("error", error);
        }

        req.getRequestDispatcher("/customer/home.jsp").forward(req, resp);
    }
}
