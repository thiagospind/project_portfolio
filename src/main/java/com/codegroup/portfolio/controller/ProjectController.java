package com.codegroup.portfolio.controller;

import com.codegroup.portfolio.dto.project.ProjectFilterDTO;
import com.codegroup.portfolio.dto.project.ProjectRequestDTO;
import com.codegroup.portfolio.dto.project.ProjectResponseDTO;
import com.codegroup.portfolio.dto.project.ProjectStatusUpdateDTO;
import com.codegroup.portfolio.exception.handler.ApiError;
import com.codegroup.portfolio.service.ProjectService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@Tag(name = "Projetos", description = "CRUD de projetos do portfólio")
@RestController
@RequestMapping("/projects")
@Validated
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    @Operation(
            summary = "Cria um novo projeto",
            description = "Status inicial é sempre EM_ANALISE. Manager precisa ser GERENTE.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Projeto criado"),
            @ApiResponse(responseCode = "404", description = "Manager não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Manager não é GERENTE / datas inválidas",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<ProjectResponseDTO> create(@Valid @RequestBody final ProjectRequestDTO requestDTO) {
        ProjectResponseDTO created = projectService.create(requestDTO);
        return ResponseEntity.created(URI.create("/projects/" + created.id())).body(created);
    }

    @Operation(summary = "Busca um projeto por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Projeto encontrado"),
            @ApiResponse(responseCode = "404", description = "Projeto não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> findById(@PathVariable final UUID id) {
        return ResponseEntity.ok(projectService.findById(id));
    }

    @Operation(summary = "Lista projetos paginados com filtros opcionais")
    @GetMapping
    public ResponseEntity<Page<ProjectResponseDTO>> findAll(
            @ParameterObject final ProjectFilterDTO filter,
            @ParameterObject final Pageable pageable) {
        return ResponseEntity.ok(projectService.findAll(filter, pageable));
    }

    @Operation(
            summary = "Atualiza um projeto existente",
            description = "Não altera o status. Para mudança de status, use PATCH /{id}/status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Projeto atualizado"),
            @ApiResponse(responseCode = "404", description = "Projeto ou manager não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Manager não é GERENTE",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> update(
            @PathVariable final UUID id,
            @Valid @RequestBody final ProjectRequestDTO requestDTO) {
        return ResponseEntity.ok(projectService.update(id, requestDTO));
    }

    @Operation(
            summary = "Altera o status do projeto",
            description = "Respeita a sequência: EM_ANALISE → ANALISE_REALIZADA → ANALISE_APROVADA → "
                    + "INICIADO → PLANEJADO → EM_ANDAMENTO → ENCERRADO. CANCELADO pode ser aplicado a qualquer momento. "
                    + "Para transicionar para INICIADO o projeto precisa ter ao menos 1 membro alocado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status atualizado"),
            @ApiResponse(responseCode = "404", description = "Projeto não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Transição inválida na sequência de status "
                    + "ou tentativa de iniciar projeto sem alocações",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<ProjectResponseDTO> updateStatus(
            @PathVariable final UUID id,
            @Valid @RequestBody final ProjectStatusUpdateDTO statusDTO) {
        return ResponseEntity.ok(projectService.updateStatus(id, statusDTO));
    }

    @Operation(
            summary = "Remove um projeto",
            description = "Bloqueado quando o status é INICIADO, EM_ANDAMENTO ou ENCERRADO.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Projeto removido"),
            @ApiResponse(responseCode = "404", description = "Projeto não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Status atual não permite deleção",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable final UUID id) {
        projectService.delete(id);
    }
}
