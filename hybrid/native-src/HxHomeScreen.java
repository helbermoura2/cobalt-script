package com.liskovsoft.smartyoutubetv2.tv.ui.browse;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.errors.ErrorFragmentData;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.BrowseView;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.common.utils.ClickbaitRemover;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tela inicial independente HX TV. Sem BrowseFragment / WebView / máscara.
 * Compartilha apenas os dados, contas e a reprodução do SmartTube.
 */
public final class HxHomeScreen extends FrameLayout implements BrowseView {
    private static final int PRETO=0xFF07090B, PAINEL=0xFF151921;
    private static final int BRONZE=0xFFCBA374, TEXTO=0xFFF4F5F6, SECUNDARIO=0xFFAEB6C0;
    private static final int FOCO=0xFF35291E;
    private final BrowsePresenter dados;
    private final List<BrowseSection> secoes=new ArrayList<>();
    private final LinkedHashMap<String,LinearLayout> fileiras=new LinkedHashMap<>();
    private final LinkedHashMap<String,List<Video>> grupos=new LinkedHashMap<>();
    private final LinearLayout menu;
    private final LinearLayout pagina;
    private final ScrollView rolagem;
    private final ProgressBar carregando;
    private TextView tituloDestaque, detalhesDestaque, seloDestaque;
    private ImageView imagemDestaque;
    private View primeiroCartao;
    private Video videoDestaque;
    private int secaoAtual=-1;
    // Guarda a posição real do cursor, inclusive ao abrir outro Activity de vídeo.
    private final LinkedHashMap<String,View> cartoesPorId=new LinkedHashMap<>();
    private String ultimoVideoFocado;
    private View ultimoControleFocado;
    private int ultimaSecaoFocada=-1;
    private boolean focoPendente;
    private boolean estavaNoMenu;
    private boolean voltarDaReproducao;

    private int dp(float valor){ return Math.round(getResources().getDisplayMetrics().density*valor); }

    private GradientDrawable fundo(int cor, int contorno, int raio){
        GradientDrawable forma=new GradientDrawable();
        forma.setColor(cor);
        forma.setCornerRadius(dp(raio));
        if(contorno!=0)forma.setStroke(dp(1.4f),contorno);
        return forma;
    }
    private TextView texto(CharSequence valor,int tamanho,int cor,boolean negrito){
        TextView t=new TextView(getContext());
        t.setText(valor);
        t.setTextSize(tamanho);
        t.setTextColor(cor);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setTypeface(Typeface.create(negrito?"sans-serif-medium":"sans-serif",
                negrito?Typeface.BOLD:Typeface.NORMAL));
        return t;
    }
    private void margem(View v,int a,int b,int c,int d){
        v.setPadding(dp(a),dp(b),dp(c),dp(d));
    }
    private void foco(View v){
        v.setFocusable(true);
        v.setBackground(fundo(PAINEL,0,12));
        v.setOnFocusChangeListener((item,selecionado)->{
            item.setBackground(fundo(selecionado?FOCO:PAINEL,
                    selecionado?BRONZE:0,12));
            if(Build.VERSION.SDK_INT>=21)item.setElevation(dp(selecionado?4:0));
        });
    }
    private void executar(Runnable tarefa){
        if(Looper.myLooper()==Looper.getMainLooper())tarefa.run();
        else post(tarefa);
    }

    public HxHomeScreen(Context contexto){
        super(contexto);
        dados=BrowsePresenter.instance(contexto);
        setBackgroundColor(PRETO);
        setFocusableInTouchMode(true);

        LinearLayout coluna=new LinearLayout(contexto);
        coluna.setOrientation(LinearLayout.VERTICAL);
        coluna.setBackgroundColor(PRETO);
        addView(coluna,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout topo=new LinearLayout(contexto);
        topo.setGravity(Gravity.CENTER_VERTICAL);
        margem(topo,28,9,25,7);
        coluna.addView(topo,new LinearLayout.LayoutParams(-1,dp(64)));

        TextView marca=texto("HX",28,BRONZE,true);
        topo.addView(marca);
        TextView complemento=texto("  TV",28,TEXTO,true);
        topo.addView(complemento);
        TextView lema=texto("     SEU UNIVERSO",10,SECUNDARIO,true);
        lema.setLetterSpacing(0.13f);
        topo.addView(lema);
        topo.addView(new View(contexto),new LinearLayout.LayoutParams(0,1,1));

        TextView pesquisa=texto("⌕  PESQUISAR",13,TEXTO,true);
        margem(pesquisa,13,9,13,9);foco(pesquisa);
        pesquisa.setOnClickListener(v->
                SearchPresenter.instance(getContext()).startSearch(null));
        topo.addView(pesquisa);

        TextView antigo=texto("  MODO CLÁSSICO  ",11,SECUNDARIO,true);
        margem(antigo,11,9,11,9);foco(antigo);
        antigo.setOnClickListener(v->{
            if(getContext() instanceof BrowseActivity)
                ((BrowseActivity)getContext()).abrirModoClassico();
        });
        topo.addView(antigo);

        View separador=new View(contexto);
        separador.setBackgroundColor(0xFF32281D);
        coluna.addView(separador,new LinearLayout.LayoutParams(-1,dp(1)));

        HorizontalScrollView barra=new HorizontalScrollView(contexto);
        barra.setHorizontalScrollBarEnabled(false);
        margem(barra,25,7,16,7);
        coluna.addView(barra,new LinearLayout.LayoutParams(-1,dp(60)));
        menu=new LinearLayout(contexto);
        menu.setGravity(Gravity.CENTER_VERTICAL);
        menu.setOrientation(LinearLayout.HORIZONTAL);
        barra.addView(menu,new HorizontalScrollView.LayoutParams(-2,-1));

        rolagem=new ScrollView(contexto);
        rolagem.setFillViewport(false);
        rolagem.setVerticalScrollBarEnabled(false);
        coluna.addView(rolagem,new LinearLayout.LayoutParams(-1,0,1));
        pagina=new LinearLayout(contexto);
        pagina.setOrientation(LinearLayout.VERTICAL);
        margem(pagina,27,6,27,30);
        rolagem.addView(pagina,new ScrollView.LayoutParams(-1,-2));
        TextView inicio=texto("SELECIONE UMA CATEGORIA",16,SECUNDARIO,true);
        margem(inicio,0,30,0,20);pagina.addView(inicio);

        carregando=new ProgressBar(contexto);
        FrameLayout.LayoutParams progresso=new FrameLayout.LayoutParams(
                dp(42),dp(42),Gravity.CENTER);
        addView(carregando,progresso);
        carregando.setVisibility(View.GONE);
    }

    private void renovarMenu(){
        menu.removeAllViews();
        for(int i=0;i<secoes.size();i++){
            final int indice=i;
            TextView aba=texto(secoes.get(i).getTitle(),14,TEXTO,true);
            aba.setSingleLine(true);
            aba.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            aba.setMarqueeRepeatLimit(-1);
            margem(aba,15,7,15,7);
            aba.setFocusable(true);
            aba.setOnFocusChangeListener((v,ativo)->{
                aba.setSelected(ativo);
                aba.setBackground(fundo((ativo||secaoAtual==indice)?FOCO:PAINEL,
                        (ativo||secaoAtual==indice)?BRONZE:0,10));
            });
            aba.setOnClickListener(v->abrirSecao(indice));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(42));
            lp.rightMargin=dp(9);
            menu.addView(aba,lp);
        }
        marcarMenu();
    }
    private void marcarMenu(){
        for(int i=0;i<menu.getChildCount();i++)
            menu.getChildAt(i).setBackground(fundo(i==secaoAtual?FOCO:PAINEL,
                    i==secaoAtual?BRONZE:0,10));
    }
    private void limparConteudo(){
        pagina.removeAllViews();
        fileiras.clear();
        grupos.clear();
        primeiroCartao=null;
        cartoesPorId.clear();
        videoDestaque=null;
        tituloDestaque=null;
        detalhesDestaque=null;
        imagemDestaque=null;
        seloDestaque=null;
        rolagem.scrollTo(0,0);
    }
    private void abrirSecao(int indice){
        if(indice<0||indice>=secoes.size())return;
        // Ao trocar de categoria, o foco deve seguir a nova categoria.
        if(secaoAtual!=indice){
            ultimoVideoFocado=null;
            ultimoControleFocado=null;
            focoPendente=false;
        }
        secaoAtual=indice;
        limparConteudo();
        marcarMenu();
        TextView aviso=texto("CARREGANDO SEU CONTEÚDO...",15,SECUNDARIO,true);
        margem(aviso,0,40,0,10);pagina.addView(aviso);
        dados.onSectionFocused(secoes.get(indice).getId());
    }
    private String chave(VideoGroup g){
        return g.getId()+"/"+(g.getTitle()==null?"Vídeos":g.getTitle());
    }
    private void destaque(Video primeiro){
        LinearLayout exterior=new LinearLayout(getContext());
        exterior.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams vertical=new LinearLayout.LayoutParams(-1,-2);
        vertical.bottomMargin=dp(20);
        pagina.addView(exterior,0,vertical);
        TextView rotulo=texto("SELEÇÃO HX TV",12,BRONZE,true);
        rotulo.setLetterSpacing(0.16f);
        margem(rotulo,0,3,0,11);
        exterior.addView(rotulo);

        LinearLayout quadro=new LinearLayout(getContext());
        quadro.setGravity(Gravity.CENTER_VERTICAL);
        quadro.setOrientation(LinearLayout.HORIZONTAL);
        quadro.setClipToOutline(Build.VERSION.SDK_INT>=21);
        quadro.setBackground(fundo(0xFF101823,0,16));
        exterior.addView(quadro,new LinearLayout.LayoutParams(-1,dp(222)));

        LinearLayout lateral=new LinearLayout(getContext());
        lateral.setOrientation(LinearLayout.VERTICAL);
        lateral.setGravity(Gravity.CENTER_VERTICAL);
        margem(lateral,23,10,15,10);
        quadro.addView(lateral,new LinearLayout.LayoutParams(0,-1,45));
        seloDestaque=texto("HX TV  /  PARA VOCÊ",11,BRONZE,true);
        seloDestaque.setLetterSpacing(0.13f);
        lateral.addView(seloDestaque);

        tituloDestaque=texto("",26,TEXTO,true);
        tituloDestaque.setMaxLines(3);
        tituloDestaque.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.topMargin=dp(9);
        lateral.addView(tituloDestaque,lp);

        detalhesDestaque=texto("",12,SECUNDARIO,false);
        detalhesDestaque.setMaxLines(2);
        detalhesDestaque.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,-2);
        mp.topMargin=dp(10);
        lateral.addView(detalhesDestaque,mp);

        TextView indicacao=texto("SELECIONE UM VÍDEO E APERTE OK",10,BRONZE,true);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);
        hp.topMargin=dp(14);lateral.addView(indicacao,hp);

        imagemDestaque=new ImageView(getContext());
        imagemDestaque.setScaleType(ImageView.ScaleType.CENTER_CROP);
        quadro.addView(imagemDestaque,new LinearLayout.LayoutParams(0,-1,55));
        atualizarDestaque(primeiro);
    }
    private void atualizarDestaque(Video video){
        if(video==null||tituloDestaque==null)return;
        videoDestaque=video;
        tituloDestaque.setText(video.getTitle());
        detalhesDestaque.setText(video.getSecondTitle());
        seloDestaque.setText(video.isLive?"●  AO VIVO":"HX TV  /  PARA VOCÊ");
        Glide.with(getContext())
             .load(ClickbaitRemover.updateThumbnail(video,
                     MainUIData.instance(getContext()).getThumbQuality()))
             .diskCacheStrategy(DiskCacheStrategy.ALL)
             .error(R.drawable.card_placeholder).into(imagemDestaque);
    }
    private void desenharFileira(String chave,String nome,List<Video> videos,boolean principal){
        LinearLayout faixa=fileiras.get(chave);
        if(faixa==null){
            LinearLayout bloco=new LinearLayout(getContext());
            bloco.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);
            bp.bottomMargin=dp(21);pagina.addView(bloco,bp);
            TextView cabecalho=texto(principal?"DESCUBRA AGORA":nome,17,TEXTO,true);
            margem(cabecalho,0,0,0,11);bloco.addView(cabecalho);
            HorizontalScrollView deslizar=new HorizontalScrollView(getContext());
            deslizar.setHorizontalScrollBarEnabled(false);
            deslizar.setClipToPadding(false);
            bloco.addView(deslizar);
            faixa=new LinearLayout(getContext());
            faixa.setOrientation(LinearLayout.HORIZONTAL);
            deslizar.addView(faixa);
            fileiras.put(chave,faixa);
        }
        faixa.removeAllViews();
        int n=0;
        for(Video video:videos){
            if(n>=24)break;
            final int pos=n++;
            final String referencia=chave+"|"+(video.videoId==null?
                    "id:"+video.id:"vid:"+video.videoId);
            LinearLayout cartao=new LinearLayout(getContext());
            cartao.setOrientation(LinearLayout.VERTICAL);
            foco(cartao);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(
                    dp(principal?191:213),dp(principal?169:186));
            cp.rightMargin=dp(12);faixa.addView(cartao,cp);
            // Selo sobre a miniatura verdadeira, sem mascarar a interface.
            FrameLayout capa=new FrameLayout(getContext());
            cartao.addView(capa,new LinearLayout.LayoutParams(-1,
                    dp(principal?107:119)));
            ImageView miniatura=new ImageView(getContext());
            miniatura.setScaleType(ImageView.ScaleType.CENTER_CROP);
            capa.addView(miniatura,new FrameLayout.LayoutParams(-1,-1));
            if(video.isLive && !video.isLiveEnd && !video.isUpcoming){
                TextView aoVivo=texto("● AO VIVO",10,TEXTO,true);
                margem(aoVivo,9,5,9,5);
                aoVivo.setBackground(fundo(0xF2BB1E2D,0,7));
                FrameLayout.LayoutParams selo=new FrameLayout.LayoutParams(
                        -2,-2,Gravity.TOP|Gravity.END);
                selo.setMargins(0,dp(8),dp(8),0);
                capa.addView(aoVivo,selo);
            }else if(video.isUpcoming){
                TextView emBreve=texto("EM BREVE",10,TEXTO,true);
                margem(emBreve,9,5,9,5);
                emBreve.setBackground(fundo(0xD6384958,0,7));
                FrameLayout.LayoutParams selo=new FrameLayout.LayoutParams(
                        -2,-2,Gravity.TOP|Gravity.END);
                selo.setMargins(0,dp(8),dp(8),0);
                capa.addView(emBreve,selo);
            }
            Glide.with(getContext())
                    .load(ClickbaitRemover.updateThumbnail(video,
                            MainUIData.instance(getContext()).getThumbQuality()))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(R.drawable.card_placeholder)
                    .into(miniatura);
            TextView nomeCartao=texto(video.getTitle(),13,TEXTO,true);
            nomeCartao.setMaxLines(2);
            nomeCartao.setEllipsize(TextUtils.TruncateAt.END);
            margem(nomeCartao,8,5,8,0);
            cartao.addView(nomeCartao,new LinearLayout.LayoutParams(-1,0,1));
            TextView dadosCartao=texto(video.getSecondTitle(),10,SECUNDARIO,false);
            dadosCartao.setSingleLine(true);
            dadosCartao.setEllipsize(TextUtils.TruncateAt.END);
            margem(dadosCartao,8,0,8,7);
            cartao.addView(dadosCartao,new LinearLayout.LayoutParams(-1,dp(24)));
            cartoesPorId.put(referencia,cartao);
            cartao.setOnFocusChangeListener((v,selecionado)->{
                v.setBackground(fundo(selecionado?FOCO:PAINEL,
                        selecionado?BRONZE:0,12));
                if(selecionado){
                    ultimoVideoFocado=referencia;
                    ultimoControleFocado=v;
                    ultimaSecaoFocada=secaoAtual;
                    estavaNoMenu=false;
                }
                nomeCartao.setSelected(selecionado);
                nomeCartao.setEllipsize(selecionado?
                        TextUtils.TruncateAt.MARQUEE:TextUtils.TruncateAt.END);
                nomeCartao.setMarqueeRepeatLimit(-1);
                if(selecionado){
                    dados.onVideoItemSelected(video);
                    if(principal)atualizarDestaque(video);
                    if(pos>=videos.size()-1)dados.onScrollEnd(video);
                }
            });
            cartao.setOnClickListener(v->{
                guardarFocoAntesDeSair();
                dados.onVideoItemClicked(video);
            });
            cartao.setOnLongClickListener(v->{
                dados.onVideoItemLongClicked(video);return true;
            });
            if(primeiroCartao==null)primeiroCartao=cartao;
        }
        if(focoPendente && ultimaSecaoFocada==secaoAtual){
            post(this::tentarRestaurarFoco);
        }
    }
    private void desenharGrupo(VideoGroup grupo){
        if(grupo==null)return;
        if(grupo.getAction()==VideoGroup.ACTION_REPLACE)limparConteudo();
        String id=chave(grupo);
        if(grupo.getAction()==VideoGroup.ACTION_REMOVE){
            grupos.remove(id);
            return;
        }
        List<Video> videos=grupos.get(id);
        if(videos==null){videos=new ArrayList<>();grupos.put(id,videos);}
        if(grupo.getAction()==VideoGroup.ACTION_SYNC)videos.clear();
        if(grupo.getAction()==VideoGroup.ACTION_PREPEND){
            List<Video> novo=new ArrayList<>(grupo.getVideos());
            for(Video v:videos)if(!novo.contains(v))novo.add(v);
            videos.clear();videos.addAll(novo);
        }else{
            for(Video v:grupo.getVideos())if(!videos.contains(v))videos.add(v);
        }
        if(videos.isEmpty())return;
        boolean principal=grupos.keySet().iterator().next().equals(id);
        if(principal&&videoDestaque==null){
            pagina.removeAllViews();
            destaque(videos.get(0));
        }
        desenharFileira(id,grupo.getTitle()==null?"Para você":grupo.getTitle(),
                videos,principal);
    }
    private void desenharConfiguracoes(SettingsGroup grupo){
        limparConteudo();
        TextView titulo=texto("AJUSTES DO HX TV",24,TEXTO,true);
        margem(titulo,0,12,0,17);
        pagina.addView(titulo);
        if(grupo==null||grupo.getItems()==null)return;
        for(SettingsItem opcao:grupo.getItems()){
            TextView item=texto(opcao.title,16,TEXTO,true);
            margem(item,18,12,18,12);foco(item);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(56));
            lp.bottomMargin=dp(8);pagina.addView(item,lp);
            item.setOnClickListener(v->{if(opcao.onClick!=null)opcao.onClick.run();});
            if(primeiroCartao==null)primeiroCartao=item;
        }
        if(focoPendente)post(this::tentarRestaurarFoco);
    }

    @Override public void addSection(int indice,BrowseSection secao){
        executar(()->{
            if(secao==null)return;
            for(int i=secoes.size()-1;i>=0;i--)
                if(secoes.get(i).getId()==secao.getId())secoes.remove(i);
            int destino=indice<0||indice>secoes.size()?secoes.size():indice;
            secoes.add(destino,secao);
            renovarMenu();
        });
    }
    @Override public void removeSection(BrowseSection secao){
        executar(()->{
            if(secao!=null)
                for(int i=secoes.size()-1;i>=0;i--)
                    if(secoes.get(i).getId()==secao.getId())secoes.remove(i);
            renovarMenu();
        });
    }
    @Override public void removeAllSections(){
        executar(()->{secoes.clear();secaoAtual=-1;renovarMenu();});
    }
    @Override public void selectSection(int indice,boolean focoConteudo){
        executar(()->{
            if(secoes.isEmpty())return;
            abrirSecao(Math.min(Math.max(0,indice),secoes.size()-1));
            if(focoConteudo)postDelayed(this::focusOnContent,400);
        });
    }
    @Override public void updateSection(VideoGroup grupo){
        executar(()->{
            desenharGrupo(grupo);
            carregando.setVisibility(View.GONE);
        });
    }
    @Override public void updateSection(SettingsGroup grupo){
        executar(()->{
            desenharConfiguracoes(grupo);
            carregando.setVisibility(View.GONE);
        });
    }
    @Override public void clearSection(BrowseSection secao){
        executar(this::limparConteudo);
    }
    @Override public void selectSectionItem(int indice){focusOnContent();}
    @Override public void selectSectionItem(Video video){focusOnContent();}
    @Override public void focusOnContent(){
        executar(()->{if(primeiroCartao!=null)primeiroCartao.requestFocus();});
    }
    @Override public void showError(ErrorFragmentData erro){
        executar(()->{
            limparConteudo();
            TextView msg=texto(erro==null?"Não foi possível carregar este conteúdo":
                    erro.getMessage(),17,TEXTO,true);
            margem(msg,5,25,0,20);pagina.addView(msg);
            if(erro!=null && erro.getActionText()!=null){
                TextView tentar=texto(erro.getActionText(),15,BRONZE,true);
                margem(tentar,12,10,12,10);foco(tentar);
                tentar.setOnClickListener(v->erro.onAction());
                pagina.addView(tentar);
            }
        });
    }
    @Override public void showProgressBar(boolean mostrar){
        executar(()->carregando.setVisibility(mostrar?View.VISIBLE:View.GONE));
    }
    @Override public boolean isProgressBarShowing(){
        return carregando.getVisibility()==View.VISIBLE;
    }
    @Override public boolean isEmpty(){return grupos.isEmpty();}
    @Override public void updateBadge(){} // Marca HX já é nativa no cabeçalho.

    /** Conserva a seleção antes do player tomar o foco da janela. */
    public void guardarFocoAntesDeSair(){
        View ativo=findFocus();
        if(ativo!=null && ativo!=this){
            ultimoControleFocado=ativo;
            estavaNoMenu=menu.hasFocus();
            ultimaSecaoFocada=secaoAtual;
        }
        voltarDaReproducao=true;
        focoPendente=true;
    }

    /** Recupera o foco quando a janela da TCL volta após a reprodução. */
    public void restaurarFocoAposReproducao(){
        if(!voltarDaReproducao)return;
        focoPendente=true;
        postDelayed(this::tentarRestaurarFoco,100);
        postDelayed(this::tentarRestaurarFoco,340);
    }

    private void tentarRestaurarFoco(){
        if(!focoPendente || !isAttachedToWindow() || !hasWindowFocus())return;
        View alvo=null;
        if(ultimoControleFocado!=null && ultimoControleFocado.isAttachedToWindow()
                && ultimoControleFocado.isShown() && ultimoControleFocado.isFocusable()){
            alvo=ultimoControleFocado;
        }else if(ultimaSecaoFocada==secaoAtual && ultimoVideoFocado!=null){
            alvo=cartoesPorId.get(ultimoVideoFocado);
        }
        if(alvo==null && estavaNoMenu && menu.getChildCount()>0){
            alvo=menu.getChildAt(Math.max(0,Math.min(secaoAtual,
                    menu.getChildCount()-1)));
        }
        if(alvo==null)alvo=primeiroCartao;
        if(alvo!=null && alvo.isShown() && alvo.requestFocus()){
            focoPendente=false;
            voltarDaReproducao=false;
        }
    }

    /** Voltar, quando dentro dos vídeos, retorna à navegação horizontal. */
    public boolean voltarAoMenu(){
        if(menu.getChildCount()>0&&!menu.hasFocus()){
            View categoria=menu.getChildAt(Math.max(0,Math.min(secaoAtual,
                    menu.getChildCount()-1)));
            categoria.requestFocus();
            ultimoControleFocado=categoria;
            estavaNoMenu=true;
            ultimaSecaoFocada=secaoAtual;
            return true;
        }
        return false;
    }
}
