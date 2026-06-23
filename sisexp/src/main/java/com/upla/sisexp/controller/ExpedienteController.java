package com.upla.sisexp.controller;

import com.upla.sisexp.dto.CambiarEstadoDTO;
import com.upla.sisexp.dto.DocumentoDTO;
import com.upla.sisexp.dto.ExpedienteFormDTO;
import com.upla.sisexp.enums.EstadoExpediente;
import com.upla.sisexp.enums.Naturaleza;
import com.upla.sisexp.enums.TipoDocumento;
import com.upla.sisexp.enums.Urgencia;
import com.upla.sisexp.exception.BusinessException;
import com.upla.sisexp.model.*;
import com.upla.sisexp.security.CustomUserDetails;
import com.upla.sisexp.service.ExpedienteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/expedientes")
public class ExpedienteController {

    private final ExpedienteService expedienteService;

    public ExpedienteController(ExpedienteService expedienteService) {
        this.expedienteService = expedienteService;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("expedientes", expedienteService.listar(user));
        model.addAttribute("estados", EstadoExpediente.values());
        return "expedientes/lista";
    }

    @GetMapping("/nuevo")
    public String mostrarFormulario(Model model) {
        model.addAttribute("formDTO", new ExpedienteFormDTO());
        model.addAttribute("actividades", expedienteService.getActividadesActivas());
        model.addAttribute("urgencias", Urgencia.values());
        model.addAttribute("naturalezas", Naturaleza.values());
        return "expedientes/formulario";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("formDTO") ExpedienteFormDTO dto,
            BindingResult result,
            @AuthenticationPrincipal CustomUserDetails user,
            Model model,
            RedirectAttributes redirect) {

        if (result.hasErrors()) {
            model.addAttribute("actividades", expedienteService.getActividadesActivas());
            model.addAttribute("urgencias", Urgencia.values());
            model.addAttribute("naturalezas", Naturaleza.values());
            return "expedientes/formulario";
        }

        try {
            Expediente exp = expedienteService.crear(dto, user);
            redirect.addFlashAttribute("success", "Expediente creado: " + exp.getCodigo());
            return "redirect:/expedientes/" + exp.getId();
        } catch (BusinessException e) {
            model.addAttribute("actividades", expedienteService.getActividadesActivas());
            model.addAttribute("urgencias", Urgencia.values());
            model.addAttribute("naturalezas", Naturaleza.values());
            result.rejectValue("actividadPoiId", "error.dto", e.getMessage());
            return "expedientes/formulario";
        }
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Expediente exp = expedienteService.obtenerConLogs(id);
        model.addAttribute("expediente", exp);
        model.addAttribute("historial", expedienteService.getHistorial(id));
        model.addAttribute("documentos", expedienteService.getDocumentos(id));
        model.addAttribute("cambiarEstadoDTO", new CambiarEstadoDTO());
        model.addAttribute("estadosExpediente", EstadoExpediente.values());
        model.addAttribute("tiposDocumento", TipoDocumento.values());
        return "expedientes/detalle";
    }

    @PostMapping("/{id}/estado")
    public String actualizarEstado(@PathVariable Long id,
            @ModelAttribute CambiarEstadoDTO dto,
            @AuthenticationPrincipal CustomUserDetails user,
            RedirectAttributes redirect) {

        try {
            expedienteService.actualizarEstado(id, dto, user);
            redirect.addFlashAttribute("success", "Estado actualizado a: " + dto.getNuevoEstado());
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/expedientes/" + id;
    }

    @PostMapping("/{id}/documentos")
    public String subirDocumento(@PathVariable Long id,
            @RequestParam TipoDocumento tipo,
            @RequestParam("archivo") MultipartFile archivo,
            RedirectAttributes redirect) {

        DocumentoDTO dto = new DocumentoDTO();
        dto.setTipo(tipo);
        dto.setArchivo(archivo);

        try {
            expedienteService.subirDocumento(id, dto);
            redirect.addFlashAttribute("success", "Documento subido correctamente");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/expedientes/" + id;
    }

    @PostMapping("/documentos/{documentoId}/eliminar")
    public String eliminarDocumento(@PathVariable Long documentoId,
            @RequestParam Long expedienteId,
            RedirectAttributes redirect) {
        try {
            expedienteService.eliminarDocumento(documentoId);
            redirect.addFlashAttribute("success", "Documento eliminado");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/expedientes/" + expedienteId;
    }

    // AJAX endpoints
    @GetMapping("/api/necesidades/por-actividad/{actividadId}")
    @ResponseBody
    public ResponseEntity<?> necesidadesPorActividad(@PathVariable Long actividadId) {
        try {
            var necesidades = expedienteService.getNecesidadesPorActividad(actividadId);
            var result = necesidades.stream()
                    .map(n -> Map.of(
                            "id", n.getId(),
                            "nombre", n.getNombre(),
                            "precioEstimado", n.getPrecioEstimado(),
                            "tipo", n.getTipo().name(),
                            "cantidadDisponible", n.getCantidadDisponible()
                    ))
                    .toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/disponibilidad/{actividadId}/{necesidadId}")
    @ResponseBody
    public ResponseEntity<?> disponibilidad(@PathVariable Long actividadId,
            @PathVariable Long necesidadId) {
        try {
            return ResponseEntity.ok(expedienteService.getDisponibilidad(actividadId, necesidadId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
