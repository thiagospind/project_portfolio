package com.codegroup.portfolio.controller;

import com.codegroup.portfolio.dto.member.MemberRequestDTO;
import com.codegroup.portfolio.dto.member.MemberResponseDTO;
import com.codegroup.portfolio.exception.handler.ApiError;
import com.codegroup.portfolio.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@Tag(name = "Membros", description = "Mock de members (simula uma API externa), permite cadastrar e consultar")
@RestController
@RequestMapping("/external/members")
@Validated
@RequiredArgsConstructor
public class MemberExternalController {

    private final MemberService memberService;

    @Operation(summary = "Cria um membro")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "Membro criado"))
    @PostMapping
    public ResponseEntity<MemberResponseDTO> create(@Valid @RequestBody final MemberRequestDTO requestDTO) {
        MemberResponseDTO created = memberService.create(requestDTO);
        return ResponseEntity.created(URI.create("/external/members/" + created.id())).body(created);
    }

    @Operation(summary = "Busca um membro por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Membro encontrado"),
            @ApiResponse(responseCode = "404", description = "Membro não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<MemberResponseDTO> findById(@PathVariable final UUID id) {
        return ResponseEntity.ok(memberService.findById(id));
    }

    @Operation(summary = "Lista membros paginados")
    @GetMapping
    public ResponseEntity<Page<MemberResponseDTO>> findAll(@ParameterObject final Pageable pageable) {
        return ResponseEntity.ok(memberService.findAll(pageable));
    }
}
