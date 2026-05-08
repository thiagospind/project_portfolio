package com.codegroup.portfolio.controller;

import com.codegroup.portfolio.dto.member.MemberResponseDTO;
import com.codegroup.portfolio.exception.handler.ApiError;
import com.codegroup.portfolio.service.ProjectAllocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Alocações", description = "Associação de membros a projetos")
@RestController
@RequestMapping("/projects/{projectId}/allocations")
@Validated
@RequiredArgsConstructor
public class ProjectAllocationController {

    private final ProjectAllocationService allocationService;

    @Operation(
            summary = "Aloca um membro ao projeto",
            description = "Apenas FUNCIONARIO pode ser alocado. Limite: 10 membros por projeto e "
                    + "3 projetos ativos por membro.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Membro alocado"),
            @ApiResponse(responseCode = "404", description = "Projeto ou membro não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Membro não é FUNCIONARIO, alocação duplicada, "
                    + "projeto cheio (10) ou membro em 3 projetos ativos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{memberId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void allocate(
            @PathVariable final UUID projectId,
            @PathVariable final UUID memberId) {
        allocationService.allocate(projectId, memberId);
    }

    @Operation(summary = "Remove a alocação de um membro do projeto")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Alocação removida"),
            @ApiResponse(responseCode = "404", description = "Alocação não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deallocate(
            @PathVariable final UUID projectId,
            @PathVariable final UUID memberId) {
        allocationService.deallocate(projectId, memberId);
    }

    @Operation(summary = "Lista os membros alocados a um projeto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de membros"),
            @ApiResponse(responseCode = "404", description = "Projeto não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<List<MemberResponseDTO>> listAllocations(@PathVariable final UUID projectId) {
        return ResponseEntity.ok(allocationService.listAllocations(projectId));
    }
}
