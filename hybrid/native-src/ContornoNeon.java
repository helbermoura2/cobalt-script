package com.liskovsoft.smartyoutubetv2.tv.ui.widgets;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;

public final class ContornoNeon extends Drawable {
    private static final long VOLTA_MS=2600L;
    private final View alvo;
    private final float raio, densidade;
    private final boolean intenso;
    private final boolean exigeFoco;
    private final Paint pincel=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path perimetro=new Path(), trecho=new Path();
    private final PathMeasure medida=new PathMeasure();
    private final Runnable proximoQuadro=this::invalidateSelf;
    private final long inicio=SystemClock.uptimeMillis();
    private float comprimento;

    public ContornoNeon(View alvo,int raio,boolean intenso,boolean exigeFoco){
        this.alvo=alvo;
        this.raio=raio;
        this.intenso=intenso;
        this.exigeFoco=exigeFoco;
        densidade=alvo.getResources().getDisplayMetrics().density;
        pincel.setStyle(Paint.Style.STROKE);
        pincel.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override protected void onBoundsChange(Rect limites){
        perimetro.rewind();
        RectF borda=new RectF(limites);
        borda.inset(2.5f*densidade,2.5f*densidade);
        if(!borda.isEmpty())perimetro.addRoundRect(borda,raio,raio,Path.Direction.CW);
        medida.setPath(perimetro,true);
        comprimento=medida.getLength();
    }

    public boolean isIntenso(){return intenso;}

    @Override public void draw(Canvas tela){
        if(comprimento<=0 || (exigeFoco && !alvo.isFocused()))return;
        long agora=SystemClock.uptimeMillis();
        boolean animar=alvo.isShown() && alvo.hasWindowFocus()
                && (Build.VERSION.SDK_INT<26 || ValueAnimator.areAnimatorsEnabled());
        float ponta=animar ? ((agora-inicio)%VOLTA_MS)/(float)VOLTA_MS*comprimento
                : comprimento*.35f;
        tracar(tela,perimetro,0x2516C8FF,5f);
        tracar(tela,perimetro,intenso?0xCE57DBFF:0x8851C6E9,1.5f);
        desenharArco(tela,ponta,0.15f,0x4033CFFF,5f);
        desenharArco(tela,ponta,0.11f,intenso?0xE353DBFF:0xAE53DBFF,2.8f);
        desenharArco(tela,ponta,0.045f,intenso?0xFFFFFFFF:0xE0B8F6FF,1.65f);
        if(animar){
            unscheduleSelf(proximoQuadro);
            scheduleSelf(proximoQuadro,agora+32L);
        }
    }

    private void desenharArco(Canvas tela,float ponta,float fracao,int cor,float espessura){
        trecho.rewind();
        float inicioTrecho=ponta-comprimento*fracao;
        if(inicioTrecho<0){
            medida.getSegment(comprimento+inicioTrecho,comprimento,trecho,true);
            medida.getSegment(0,ponta,trecho,false);
        }else{
            medida.getSegment(inicioTrecho,ponta,trecho,true);
        }
        tracar(tela,trecho,cor,espessura);
    }

    private void tracar(Canvas tela,Path caminho,int cor,float espessura){
        pincel.setColor(cor);
        pincel.setStrokeWidth(espessura*densidade);
        tela.drawPath(caminho,pincel);
    }

    @Override public void setAlpha(int alpha){}
    @Override public void setColorFilter(ColorFilter filtro){}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
