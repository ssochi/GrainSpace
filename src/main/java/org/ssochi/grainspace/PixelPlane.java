package org.ssochi.grainspace;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PixelPlane extends JFrame{
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


    private static final int DEFAULT_BUFFER_CAPACITY = 10;
    private boolean modifyBasic = false;

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
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        imageMirror = new int[width][height];
        this.setSize(width, height);
        this.setVisible(true);

        addKeyListener(new KeyListenerAdapter(){
            @Override
            public void keyPressed(KeyEvent e) {
                setInfo();
                var key = e.getKeyChar();
                switch (key){
                    case 'w':
                        basicY = modifyBasic(basicY,-zoom,realH,height);
                        break;
                    case 's':
                        basicY = modifyBasic(basicY,zoom,realH,height);
                        break;
                    case 'a':
                        basicX = modifyBasic(basicX,-zoom,realW,width);
                        break;
                    case 'd':
                        basicX = modifyBasic(basicX,zoom,realW,width);
                        break;
                    case 'i':
                        skip++;
                        break;
                    case 'o':
                        skip = Math.max(1,skip - 1);
                        break;
                }
            }

            private int modifyBasic(int basic, int zoom,int max,int gap) {
                var abs = Math.max(1,height / Math.abs(zoom) / 25);
                var next = zoom > 0 ? basic + abs : basic - abs;
                if (next + gap / zoom > max || next < 0){
                    return basic;
                }
                modifyBasic = true;
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
    private void controlBasic(Point point,boolean bigger) {
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
        modifyBasic = true;
    }

    private boolean isPointValid(Point point) {
        return point.x >= 0 && point.x <= realW && point.y >= 0 && point.y <= realH;
    }


    public void update(PixelInfo info) throws InterruptedException {
        repaint();
        if (skipCounter + 1 == skip){
            buf.put(info);
        }
        skipCounter = (++skipCounter) % skip;
    }

    @Override
    public void update(Graphics g) {
        super.update(g);
    }

    @Override
    public void paint(Graphics g) {
        var current = System.currentTimeMillis();
        if (paint0(g)){
            System.out.println("paint cost = " + (System.currentTimeMillis() - current) + " / " + (System.currentTimeMillis() - last));
            frame = (long) (1 / ((System.currentTimeMillis() - last) / 1000f));
            last = System.currentTimeMillis();
        }

    }
    BufferedImage image;
    int[][] imageMirror;
    private boolean paint0(Graphics g) {
        setInfo();
        try{
            if (buf.size() > 0){
                var content = buf.take();

                if (basicX > realH || basicY > realW){
                    return false;
                }
                var maxX = Math.min(basicX + width / zoom,realW);
                var maxY = Math.min(basicY + height / zoom,realH);

                var countX = maxX - basicX ;
                var countY = maxY - basicY ;
                int light = 0;
                boolean forceUpdate = false;
                if (modifyBasic){
                    modifyBasic = false;
                    for (int i = 0; i < width; i++) {
                        for (int j = 0; j < height; j++) {
                            image.setRGB(i,j,0);
                        }
                    }
                    forceUpdate = true;
                }
                for (int x = 0; x < countX; x++) {
                    for (int y = 0; y < countY; y++) {
                        for (int ix = 0; ix < zoom; ix++) {
                            for (int iy = 0; iy < zoom; iy++) {
                                light = content.getPixelArray()[x + basicX][y + basicY];
                                if(forceUpdate || imageMirror[x*zoom + ix][y*zoom + iy] != light){
                                    imageMirror[x*zoom + ix][y*zoom + iy] = light;
                                    image.setRGB(x*zoom + ix,y*zoom + iy,light);
                                }
                            }
                        }
                    }
                }
                g.drawImage(image, 0, 0, null);
                return true;
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return false;

    }
    private long lastSetInfo = System.currentTimeMillis();
    private void setInfo() {
        var t = System.currentTimeMillis();
        if (t - lastSetInfo > 1000){
            setTitle("frame=" + frame + "," + ",zoom=" + zoom + "skip=" + skip + ",bufSize=" +  buf.size() + ",操作说明 : w s a d移动,i o加减skip,鼠标左右键放大缩小");
            lastSetInfo = t;
        }
    }
}



