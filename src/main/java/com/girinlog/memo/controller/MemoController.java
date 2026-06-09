package com.girinlog.memo.controller;

import com.girinlog.memo.service.MemoService;
import com.girinlog.memo.service.MemoSummaryCreation;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
public class MemoController {

    private final MemoService memoService;

    public MemoController(MemoService memoService) {
        this.memoService = memoService;
    }

    @GetMapping("/memos")
    public MemoListResponse listMemos(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        LocalDate responseDate = memoService.defaultToday(date);
        return MemoListResponse.of(responseDate, memoService.listMemos(responseDate));
    }

    @PostMapping("/memos")
    @ResponseStatus(HttpStatus.CREATED)
    public MemoResponse createMemo(@RequestBody @Valid CreateMemoRequest request) {
        return MemoResponse.from(memoService.createMemo(request.content()));
    }

    @PostMapping("/memos/summaries")
    @ResponseStatus(HttpStatus.CREATED)
    public MemoSummaryResultResponse createMemoSummaries(
            @RequestBody(required = false) CreateMemoSummariesRequest request
    ) {
        LocalDate date = request == null ? null : request.date();
        MemoSummaryCreation creation = memoService.createMemoSummaries(date);
        return MemoSummaryResultResponse.of(creation.date(), creation.memoSummaries(), creation.nextMemo());
    }

    @GetMapping("/memo-summaries")
    public MemoSummaryListResponse listMemoSummaries(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        LocalDate responseDate = memoService.defaultToday(date);
        return MemoSummaryListResponse.of(responseDate, memoService.listMemoSummaries(responseDate));
    }
}
