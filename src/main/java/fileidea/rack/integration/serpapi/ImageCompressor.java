package fileidea.rack.integration.serpapi;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

final class ImageCompressor {

    private ImageCompressor() {
    }

    static byte[] fitUnder(byte[] bytes, int maxBytes) {
        if (bytes.length <= maxBytes) {
            return bytes;
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                return bytes;
            }
            double scale = 1.0;
            byte[] out = encode(image, 0.85f);
            while (out.length > maxBytes && scale > 0.2) {
                scale *= 0.8;
                int w = Math.max(1, (int) (image.getWidth() * scale));
                int h = Math.max(1, (int) (image.getHeight() * scale));
                BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = resized.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(image, 0, 0, w, h, null);
                g.dispose();
                out = encode(resized, 0.8f);
            }
            return out;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] encode(BufferedImage image, float quality) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream out = new MemoryCacheImageOutputStream(buf)) {
            writer.setOutput(out);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return buf.toByteArray();
    }
}
