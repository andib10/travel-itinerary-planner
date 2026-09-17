package com.app.travel_planner.controller;

import com.app.travel_planner.dto.AdminPoiResponse;
import com.app.travel_planner.dto.UpdatePoiRequest;
import com.app.travel_planner.service.AdminPoiService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin POI management
 */
@RestController
@RequestMapping("/api/admin/pois")
public class AdminPoiController {

    private final AdminPoiService adminPoiService;

    public AdminPoiController(AdminPoiService adminPoiService) {
        this.adminPoiService = adminPoiService;
    }

    @GetMapping
    public List<AdminPoiResponse> listPois(@RequestParam(required = false) String city) {
        return adminPoiService.listPois(city);
    }

    @PutMapping("/{id}")
    public AdminPoiResponse updatePoi(@PathVariable Long id, @Valid @RequestBody UpdatePoiRequest request) {
        return adminPoiService.updatePoi(id, request);
    }

    @DeleteMapping("/{id}")
    public void deletePoi(@PathVariable Long id) {
        adminPoiService.deletePoi(id);
    }
}
