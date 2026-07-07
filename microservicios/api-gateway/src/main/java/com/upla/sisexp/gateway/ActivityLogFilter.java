package com.upla.sisexp.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ActivityLogFilter implements GlobalFilter, Ordered {

    private final ActivityBuffer buffer;
    private final SecretKey key;

    private static final Pattern EXPEDIENTE_ID = Pattern.compile("/expedientes/(\\d+)");
    private static final Pattern EXPEDIENTE_CODIGO = Pattern.compile("/rastreo/(EXP-\\S+)");
    private static final Pattern NOTA_ID = Pattern.compile("/notas-modificatorias/(\\d+)");

    public ActivityLogFilter(ActivityBuffer buffer, @Value("${jwt.secret}") String secret) {
        this.buffer = buffer;
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();
        String userEmail = extractEmail(exchange);

        if (path.startsWith("/api/monitor")) {
            return chain.filter(exchange);
        }

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            int status = exchange.getResponse().getStatusCode() != null ?
                    exchange.getResponse().getStatusCode().value() : 0;
            String service = resolveService(path);
            String action = translateAction(method, path, status);
            String description = buildDescription(method, path, status, exchange, userEmail);

            buffer.add(new ActivityEvent(
                    Instant.now(), service, action, description, path, status,
                    userEmail != null ? userEmail : "anonimo"
            ));
        }));
    }

    @Override
    public int getOrder() { return 10; }

    private String extractEmail(ServerWebExchange exchange) {
        String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(auth.substring(7)).getPayload();
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveService(String path) {
        if (path.startsWith("/api/auth/") || path.startsWith("/api/usuarios") || path.equals("/api/status")
                || path.equals("/api/health"))
            return "auth-service";
        if (path.startsWith("/api/techos") || path.startsWith("/api/actividades-poi")
                || path.startsWith("/api/necesidades-pap") || path.startsWith("/api/notas-modificatorias")
                || path.startsWith("/api/dashboard") || path.startsWith("/api/reportes"))
            return "presupuesto-service";
        if (path.startsWith("/api/expedientes"))
            return "expediente-service";
        if (path.startsWith("/api/notificaciones"))
            return "notificacion-service";
        return "api-gateway";
    }

    private String translateAction(HttpMethod method, String path, int status) {
        if (status >= 400) return "ERROR";
        if (path.contains("/login") && method == HttpMethod.POST) return "LOGIN";
        if (path.contains("/expedientes/") && path.contains("/estado") && method == HttpMethod.PUT) return "ESTADO";
        if (path.contains("/expedientes/") && path.contains("/documentos") && method == HttpMethod.POST) return "SUBIR_DOC";
        if (path.contains("/notas-modificatorias/") && path.contains("/configurar")) return "CONFIGURAR";
        if (path.contains("/notas-modificatorias/") && path.contains("/rechazar")) return "RECHAZAR";
        if (path.contains("/rastreo")) return "CONSULTA";
        if (method == HttpMethod.POST) return "CREAR";
        if (method == HttpMethod.PUT) return "ACTUALIZAR";
        if (method == HttpMethod.DELETE) return "ELIMINAR";
        if (method == HttpMethod.GET) return "CONSULTAR";
        return method.name();
    }

    private String buildDescription(HttpMethod method, String path, int status, ServerWebExchange exchange, String user) {
        String who = user != null ? user : "Anonimo";
        if (status >= 500) return who + " - Error interno en " + path;
        if (status == 404) return who + " - No encontrado: " + path;
        if (status == 403) return who + " - Acceso denegado a " + path;
        if (status == 401) return who + " - Autenticacion fallida";

        if (path.contains("/login") && method == HttpMethod.POST) {
            return status == 200 ? who + " inicio sesion" : who + " - Intento de login fallido";
        }

        Matcher expId = EXPEDIENTE_ID.matcher(path);
        Matcher expCode = EXPEDIENTE_CODIGO.matcher(path);
        Matcher notaId = NOTA_ID.matcher(path);

        if (path.contains("/expedientes") && path.contains("/estado") && expId.find()) {
            String id = expId.group(1);
            return who + " cambio estado del expediente #" + id;
        }
        if (path.contains("/expedientes") && path.contains("/documentos") && expId.find()) {
            String id = expId.group(1);
            return who + " subio documento al expediente #" + id;
        }
        if (path.equals("/api/expedientes") && method == HttpMethod.POST) {
            return who + " creo un nuevo expediente";
        }
        if (path.contains("/notas-modificatorias") && path.contains("/configurar") && notaId.find()) {
            return who + " configuro nota modificatoria #" + notaId.group(1);
        }
        if (path.contains("/notas-modificatorias") && path.contains("/rechazar") && notaId.find()) {
            return who + " rechazo nota modificatoria #" + notaId.group(1);
        }
        if (path.equals("/api/notas-modificatorias") && method == HttpMethod.POST) {
            return who + " creo nota modificatoria";
        }
        if (path.contains("/rastreo") && expCode.find()) {
            return who + " consulto estado de " + expCode.group(1);
        }

        if (method == HttpMethod.POST) {
            return who + " envio " + path;
        }
        if (method == HttpMethod.PUT) {
            return who + " actualizo " + path;
        }
        if (method == HttpMethod.DELETE) {
            return who + " elimino " + path;
        }
        return who + " consulto " + path;
    }
}
