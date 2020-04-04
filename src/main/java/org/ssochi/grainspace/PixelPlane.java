package org.ssochi.grainspace;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PixelPlane extends JFrame {
    //窗口大小
    private int width,height;
    private int realW,realH;

    //屏幕基准点
    private int basicX, basicY;
    //放大系数
    private int zoom = 4;
    //待显示队列
    private BlockingQueue<PixelInfo> buf;

    private static int skip = 1;
    private int skipCounter = 0;
    private long frame = 0;
    private long last = System.currentTimeMillis();
    private boolean stop = false;


    private static final int DEFAULT_BUFFER_CAPACITY = 10;

    PixelPlane(int realW,int realH){
        super("PixelShowBox");
        this.realH = realH;
        this.realW = realW;
        buf = new LinkedBlockingQueue<>(DEFAULT_BUFFER_CAPACITY);
    }

    public void boot(){
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        var dim = Toolkit.getDefaultToolkit().getScreenSize();
        width = (int) dim.getWidth();
        height = (int) dim.getHeight();

        this.setSize(width, height);
        this.setVisible(true);

        addKeyListener(new KeyListenerAdapter(){
            @Override
            public void keyPressed(KeyEvent e) {
                setInfo();
                var key = e.getKeyChar();
                switch (key){
                    case 'w':
                        basicY = modifyBasic(basicY,-zoom,realH);
                        break;
                    case 's':
                        basicY = modifyBasic(basicY,zoom,realH);
                        break;
                    case 'a':
                        basicX = modifyBasic(basicX,-zoom,realW);
                        break;
                    case 'd':
                        basicX = modifyBasic(basicX,zoom,realW);
                        break;
                    case 'i':
                        skip++;
                        break;
                    case 'o':
                        skip = Math.max(1,skip - 1);
                        break;
                    case 'p':
                        stop = !stop;
                        repaint();
                        break;
                }
            }

            private int modifyBasic(int basic, int zoom,int max) {
                var abs = Math.max(1,height / Math.abs(zoom) / 25);
                var next = zoom > 0 ? basic + abs : basic - abs;
                if (next > max || next < 0){
                    return basic;
                }
                return next;
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setInfo();
                var point = e.getPoint();
                switch (e.getButton()){
                    case 1:
                        controlBasic(point,true);
                        break;
                    case 3:
                        if (zoom <= 1)
                            return;
                        controlBasic(point,false);
                        break;
                }
            }
        });
    }

    private int validBasic(int basic,int max) {
        if (basic < 0) return 0;
        return Math.min(basic, max);
    }
    private void controlBasic(Point point, boolean bigger) {
        point.x = (int) (basicX + point.x * 1f/ zoom);
        point.y = (int) (basicY + point.y * 1f/ zoom);

        if (!isPointValid(point)){
            return;
        }
        float deltaX = point.x - basicX;
        float deltaY = point.y - basicY;

        if (bigger){
            zoom *= 2;deltaX /= 2;deltaY /= 2;
        }else{
            zoom /= 2;deltaX *= 2;deltaY *= 2;
        }
        basicX = (int) (point.x - deltaX);
        basicY = (int) (point.y - deltaY);

        basicX = validBasic(basicX,realW);
        basicY = validBasic(basicY,realH);
    }

    private boolean isPointValid(Point point) {
        return point.x >= 0 && point.x <= realW && point.y >= 0 && point.y <= realH;
    }


    public void update(PixelInfo info) throws InterruptedException {
        if (skipCounter + 1 == skip){
            buf.put(info);
        }
        if (stop) return;
        repaint();
        skipCounter = (++skipCounter) % skip;
    }

    @Override
    public void update(Graphics g) {
        super.update(g);
    }

    @Override
    public void paint(Graphics g) {
        if (paint0(g)){
            frame = (long) (1 / ((System.currentTimeMillis() - last) / 1000f));
            last = System.currentTimeMillis();
        }

    }

    private boolean paint0(Graphics g) {
        setInfo();
        try{
            if (buf.size() > 0){
                var content = buf.take();

                int maxHeight = height / zoom;
                int maxWidth = width / zoom;

                if (basicX > realH || basicY > realW){
                    return false;
                }
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                var maxX = Math.min(basicX + maxWidth,realW);
                var maxY = Math.min(basicY + maxHeight,realH);

                var countX = maxX - basicX ;
                var countY = maxY - basicY ;
                for (int x = 0; x < countX; x++) {
                    for (int y = 0; y < countY; y++) {
                        for (int ix = 0; ix < zoom; ix++) {
                            for (int iy = 0; iy < zoom; iy++) {
                                image.setRGB(x*zoom + ix,y*zoom + iy,content.getPixelArray()[x + basicX][y + basicY]);
                            }
                        }
                    }
                }
                g.drawImage(image,0,0,null);
                return true;
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return false;

    }

    private void setInfo() {
        setTitle("frame=" + frame + "," + ",zoom=" + zoom + "skip=" + skip + ",bufSize=" +  buf.size() + ",操作说明 : w s a d移动,i o加减skip,鼠标左右键放大缩小" +
                "p暂停");
    }
}



