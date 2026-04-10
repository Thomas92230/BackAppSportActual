package com.actuSport.interfaces.rest;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class StaticContentController {

    @GetMapping(value = "/search-interface.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getSearchInterface() {
        try {
            // Essayer de lire depuis les ressources statiques
            Resource resource = new ClassPathResource("static/search-interface.html");
            if (resource.exists()) {
                String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                return ResponseEntity.ok(content);
            }
            
            // Essayer de lire depuis le répertoire racine du projet
            Path path = Paths.get("search-interface.html");
            if (Files.exists(path)) {
                String content = Files.readString(path, StandardCharsets.UTF_8);
                return ResponseEntity.ok(content);
            }
            
            // Retourner une page HTML simple avec redirection vers l'API
            String simpleHtml = createSimpleSearchInterface();
            return ResponseEntity.ok(simpleHtml);
            
        } catch (IOException e) {
            // En cas d'erreur, retourner une interface simple
            String simpleHtml = createSimpleSearchInterface();
            return ResponseEntity.ok(simpleHtml);
        }
    }
    
    private String createSimpleSearchInterface() {
        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Recherche d'Articles Sportifs</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
                    .container { max-width: 1200px; margin: 0 auto; background: white; padding: 20px; border-radius: 10px; }
                    .search-form { display: flex; gap: 10px; margin-bottom: 20px; }
                    .search-form input { padding: 10px; border: 1px solid #ddd; border-radius: 5px; }
                    .search-form button { padding: 10px 20px; background: #007bff; color: white; border: none; border-radius: 5px; cursor: pointer; }
                    .article { border: 1px solid #ddd; padding: 15px; margin-bottom: 15px; border-radius: 5px; }
                    .article h3 { margin: 0 0 10px 0; color: #333; }
                    .article img { max-width: 300px; height: auto; margin: 10px 0; border-radius: 5px; }
                    .article .meta { color: #666; font-size: 0.9em; margin-bottom: 10px; }
                    .loading { text-align: center; padding: 20px; color: #666; }
                    .error { background: #f8d7da; color: #721c24; padding: 10px; border-radius: 5px; margin-bottom: 15px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>Recherche d'Articles Sportifs</h1>
                    <div class="search-form">
                        <input type="text" id="keyword" placeholder="Mot-clé...">
                        <button onclick="performSearch()">Rechercher</button>
                        <button onclick="loadAllArticles()">Voir tous</button>
                        <label>
                            <input type="checkbox" id="showImages" checked> Images
                        </label>
                    </div>
                    <div id="results">
                        <div class="loading">Cliquez sur "Voir tous" pour charger les articles</div>
                    </div>
                </div>
                
                <script>
                    const API_BASE = 'http://localhost:8080/api/news';
                    
                    function performSearch() {
                        const keyword = document.getElementById('keyword').value;
                        const showImages = document.getElementById('showImages').checked;
                        const resultsDiv = document.getElementById('results');
                        
                        resultsDiv.innerHTML = '<div class="loading">Recherche en cours...</div>';
                        
                        if (keyword.trim()) {
                            fetch(`${API_BASE}/search/advanced?keyword=${encodeURIComponent(keyword)}&enhanced=${showImages}`)
                                .then(response => response.json())
                                .then(data => displayResults(data))
                                .catch(error => {
                                    console.error('Error:', error);
                                    resultsDiv.innerHTML = '<div class="error">Erreur lors de la recherche</div>';
                                });
                        }
                    }
                    
                    function loadAllArticles() {
                        const showImages = document.getElementById('showImages').checked;
                        const resultsDiv = document.getElementById('results');
                        
                        resultsDiv.innerHTML = '<div class="loading">Chargement...</div>';
                        
                        fetch(`${API_BASE}/enhanced?page=0&size=20`)
                            .then(response => response.json())
                            .then(data => displayResults(data))
                            .catch(error => {
                                console.error('Error:', error);
                                resultsDiv.innerHTML = '<div class="error">Erreur lors du chargement</div>';
                            });
                    }
                    
                    function displayResults(data) {
                        const resultsDiv = document.getElementById('results');
                        const showImages = document.getElementById('showImages').checked;
                        
                        let articles = Array.isArray(data) ? data : (data.content || []);
                        
                        if (articles.length === 0) {
                            resultsDiv.innerHTML = '<div class="loading">Aucun article trouvé</div>';
                            return;
                        }
                        
                        const html = articles.map(article => `
                            <div class="article">
                                <h3>${article.title}</h3>
                                <div class="meta">
                                    Source: ${article.source || 'Inconnue'} | 
                                    Date: ${new Date(article.publishedAt).toLocaleDateString('fr-FR')}
                                </div>
                                ${article.summary ? `<p>${article.summary}</p>` : ''}
                                ${article.imageUrl && showImages ? 
                                    `<img src="${article.imageUrl}" alt="${article.title}" onerror="this.style.display='none'">` : ''}
                                <div>
                                    <a href="${article.articleUrl || '#'}" target="_blank">Article complet</a>
                                </div>
                            </div>
                        `).join('');
                        
                        resultsDiv.innerHTML = html;
                    }
                    
                    // Charger tous les articles au démarrage
                    window.addEventListener('load', () => {
                        setTimeout(loadAllArticles, 100);
                    });
                </script>
            </body>
            </html>
            """;
    }
}
