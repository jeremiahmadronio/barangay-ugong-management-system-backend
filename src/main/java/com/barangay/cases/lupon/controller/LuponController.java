package com.barangay.cases.lupon.controller;

import com.barangay.cases.lupon.dto.LuponReferRalRequest;
import com.barangay.cases.lupon.dto.LuponReferralResponse;
import com.barangay.cases.lupon.service.LuponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/lupon")
@RequiredArgsConstructor
public class LuponController {

    private final LuponService luponService;


}
