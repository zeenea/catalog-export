package com.zeenea.cli.export;

import com.beust.jcommander.Parameter;
import com.zeenea.client.api.ZeeneaConfig;
import com.zeenea.client.api.ZeeneaConfigBuilder;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration de l'export.
 *
 * <h3>Détails d'implantation</h3>
 * <p>JCommander est utilisé pour analyser les arguments fournis en ligne de commande et injecter leurs valeurs
 * dans cet objet.</p>
 *
 * @see <a href="http://jcommander.org/">La documentation de JCommander</a> (en).
 */
public class ExportParams  {
    @Parameter(names = {"-o", "--output"}, description = "Output file path")
    private Path outputFile = Paths.get("zeenea-datasets.xlsx");

    @Parameter(names = {"-f", "--force", "--override"}, description = "Override existing file")
    private boolean overrideExistingOutput = false;

    @Parameter(names = {"--url"}, description = "URL de connexion à Zeenea", required = true)
    private URI uri;

    @Parameter(names = {"-u", "--user"}, description = "Utilisateur Zeenea", required = true)
    private String user;

    @Parameter(names = {"-p", "--password"}, description = "Mot de passe de l'utilisateur Zeenea", required = true, password = true)
    private String password;

    @Parameter(names = "--help", description = "Affiche le message d'aide", help = true)
    private boolean help;

    /**
     * Convert this {@code ExportConfig} to a {@code ZeeneaConfig}.
     *
     * @return a ZeeneaConfig.
     * @see ZeeneaConfig
     */
    public ZeeneaConfig toZeeneaConfig() {
        return new ZeeneaConfigBuilder()
            .uri(getUri())
            .username(getUser())
            .password(getPassword())
            .build();
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    /**
     * Chemin du fichier d'export.
     * <p>Option: {@code -o} ou {@code --output}.</p>
     *
     * @return un {@link Path}
     */
    public Path getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(Path outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * Autorise l'écrasement d'un fichier existant par l'export.
     * <p>Option: {@code -f}, {@code --force} ou {@code --override}.</p>
     *
     * @return {@code true} si le fichier d'export peut être écrasé.
     */
    public boolean isOverrideExistingOutput() {
        return overrideExistingOutput;
    }

    public void setOverrideExistingOutput(boolean overrideExistingOutput) {
        this.overrideExistingOutput = overrideExistingOutput;
    }

    public boolean isHelp() {
        return help;
    }

    public void setHelp(boolean help) {
        this.help = help;
    }

}
