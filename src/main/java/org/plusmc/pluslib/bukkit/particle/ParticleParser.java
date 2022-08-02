package org.plusmc.pluslib.bukkit.particle;

import org.bukkit.Color;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.io.InputStream;

public class ParticleParser {


    public static PlusParticleEffect parseFile(InputStream inputStream) throws IOException {
        String data = new String(inputStream.readAllBytes());
        PlusParticleEffect effect = new PlusParticleEffect();
        for (String line : data.split("\n")) {
            effect.getParticles().add(parseLine(line));
        }

        return effect;
    }


    public static PlusParticleEffect.PlusParticle parseLine(String line) {
        String[] split = line.split(":");
        if (split.length != 6)
            throw new IllegalArgumentException("Invalid particle line: " + line);

        String pos = split[0];
        String modifier = split[1];
        String size = split[2];
        String color = split[3];
        String count = split[4];
        String delta = split[5];


        return new PlusParticleEffect.PlusParticle(
                parseVector(pos),
                modifier,
                Integer.parseInt(size),
                parseColor(color),
                Integer.parseInt(count),
                parseVector(delta)
        );
    }


    public static Vector parseVector(String vectorString) {
        String[] split = vectorString.split(",");
        if (split.length != 3)
            throw new IllegalArgumentException("Invalid vector: " + vectorString);

        String x = split[0];
        String y = split[1];
        String z = split[2];
        return new Vector(Double.parseDouble(x), Double.parseDouble(y), Double.parseDouble(z));
    }

    public static Color parseColor(String colorString) {
        String[] split = colorString.split(",");
        if (split.length != 3)
            throw new IllegalArgumentException("Invalid color: " + colorString);

        String red = split[0];
        String green = split[1];
        String blue = split[2];
        return Color.fromRGB(Integer.parseInt(red), Integer.parseInt(green), Integer.parseInt(blue));
    }
}
