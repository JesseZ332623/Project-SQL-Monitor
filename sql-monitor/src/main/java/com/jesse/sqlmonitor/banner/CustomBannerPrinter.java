package com.jesse.sqlmonitor.banner;

import com.jesse.sqlmonitor.SQLMonitorApplication;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/** 自定义横幅输出工具类。*/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class CustomBannerPrinter
{
    /** 输出自定义横幅。*/
    public static void printStartupBanner()
    {
        try (InputStream is = SQLMonitorApplication.class.getClassLoader().getResourceAsStream("banner.gif"))
        {

            if (is == null)
            {
                System.out.println("banner.gif not found, skip...");
                return;
            }

            // ImageIO 能直接读取 GIF 的第一帧
            BufferedImage image = ImageIO.read(is);

            // System.out.print("\033[?25l");               // 隐藏光标
            printAsciiArt(image);                        // 直接打印第一帧
            Thread.sleep(800);                    // 可选：停留一会儿好看点
            // System.out.print("\033[?25h\033[H\033[2J"); // 恢复光标 + 清屏
            System.out.flush();

        } catch (Exception e) {
            // 静默失败，不影响启动
        }
    }

    /** 将一帧图片转化成 ASCII 艺术图像并输出。*/
    private static void
    printAsciiArt(@NotNull BufferedImage image)
    {
        final int width  = 35;
        final int height = 22;

        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        resized.getGraphics().drawImage(image.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH), 0, 0, null);

        String chars = " .:-=+*#%@";
        StringBuilder sb = new StringBuilder();

        for (int y = 0; y < height; y += 2)
        {
            // 步长 2，避免拉伸变形
            for (int x = 0; x < width; x++)
            {
                int pixel = resized.getRGB(x, y);
                int r     = (pixel >> 16) & 0xff;
                int g     = (pixel >> 8) & 0xff;
                int b     = pixel & 0xff;
                int gray  = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                int index = gray * (chars.length() - 1) / 255;

                sb.append("\033[48;2;").append(r).append(';')
                    .append(g).append(';').append(b).append("m")
                    .append(chars.charAt(index))
                    .append("\033[0m");
            }

            sb.append('\n');
        }

        System.out.print(sb); // 回到左上角覆盖打印
    }
}