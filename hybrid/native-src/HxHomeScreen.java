package com.liskovsoft.smartyoutubetv2.tv.ui.browse;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
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
import com.liskovsoft.smartyoutubetv2.common.misc.AppDataSourceManager;
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
    private static final int PRETO=0xFF06080D, PAINEL=0xFF111824;
    private static final int GELO=0xFFB5D7FF, TEXTO=0xFFF7F9FC, SECUNDARIO=0xFF9EACBF;
    private static final int FOCO=0xFF213650;
    private final BrowsePresenter dados;
    private final List<BrowseSection> secoes=new ArrayList<>();
    private final LinkedHashMap<String,LinearLayout> fileiras=new LinkedHashMap<>();
    private final LinkedHashMap<String,List<Video>> grupos=new LinkedHashMap<>();
    private final LinearLayout menu;
    private final List<View> primeiraFileira=new ArrayList<>();
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
    private View destaqueFocavel;
    private String grupoPrincipalId;
    // Quando os vídeos são carregados de modo assíncrono, entrar no conteúdo
    // deve funcionar mesmo antes de o primeiro card existir.
    private boolean entrarNoConteudoPendente;
    private LinearLayout painelLateral;
    private TextView marcaNav, subtituloNav, pesquisaNav, atualizarNav, classicoNav, alternarNav;
    private boolean menuAberto=true;
    private ValueAnimator animacaoNav;
    private final List<TextView> itensMenu=new ArrayList<>();

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
        v.setBackground(fundo(PAINEL,0,13));
        v.setOnFocusChangeListener((item,selecionado)->{
            item.setBackground(fundo(selecionado?FOCO:PAINEL,
                    selecionado?0xFFEAF5FF:0,13));
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

        // HX TV Premium: trilho lateral nativo. Nenhum botão interativo sob
        // o Picture-in-Picture/miniplayer, que aparece no canto superior direito.
        LinearLayout estrutura=new LinearLayout(contexto);
        estrutura.setOrientation(LinearLayout.HORIZONTAL);
        estrutura.setBackgroundColor(PRETO);
        addView(estrutura,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout lateral=new LinearLayout(contexto);
        lateral.setOrientation(LinearLayout.VERTICAL);
        lateral.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF1A202C,0xFF10151E,0xFF090D14}));
        margem(lateral,11,20,11,14);
        painelLateral=lateral;
        estrutura.addView(lateral,new LinearLayout.LayoutParams(dp(174),-1));

        TextView marca=texto("HX  TV",29,TEXTO,true);
        marcaNav=marca;
        marca.setLetterSpacing(0.06f);
        margem(marca,9,0,0,0);
        lateral.addView(marca);
        TextView lema=texto("A SUA EXPERIÊNCIA",9,SECUNDARIO,true);
        subtituloNav=lema;
        lema.setLetterSpacing(0.17f);
        margem(lema,9,3,0,22);
        lateral.addView(lema);

        TextView alternar=texto("‹  RECOLHER MENU",11,GELO,true);
        alternarNav=alternar;
        alternar.setContentDescription("Recolher ou expandir menu lateral");
        margem(alternar,10,9,6,9);
        foco(alternar);
        alternar.setOnClickListener(v->setPainelAberto(!menuAberto));
        LinearLayout.LayoutParams toggleLp=new LinearLayout.LayoutParams(-1,dp(36));
        toggleLp.bottomMargin=dp(12);
        lateral.addView(alternar,toggleLp);

        View linhaBronze=new View(contexto);
        linhaBronze.setBackgroundColor(0xFF2A384A);
        LinearLayout.LayoutParams divisorMenu=new LinearLayout.LayoutParams(-1,dp(1));
        divisorMenu.bottomMargin=dp(11);
        lateral.addView(linhaBronze,divisorMenu);

        ScrollView navegacao=new ScrollView(contexto);
        navegacao.setVerticalScrollBarEnabled(false);
        navegacao.setFillViewport(false);
        lateral.addView(navegacao,new LinearLayout.LayoutParams(-1,0,1));
        menu=new LinearLayout(contexto);
        menu.setGravity(Gravity.TOP);
        menu.setOrientation(LinearLayout.VERTICAL);
        navegacao.addView(menu,new ScrollView.LayoutParams(-1,-2));

        TextView pesquisa=texto("⌕  PESQUISAR",12,TEXTO,true);
        pesquisaNav=pesquisa;
        margem(pesquisa,13,10,9,10);
        foco(pesquisa);
        pesquisa.setOnClickListener(v->
                SearchPresenter.instance(getContext()).startSearch(null));
        lateral.addView(pesquisa,new LinearLayout.LayoutParams(-1,dp(42)));

        TextView atualizar=texto("↻  ATUALIZAR",12,GELO,true);
        atualizarNav=atualizar;
        margem(atualizar,13,10,9,10);
        foco(atualizar);
        atualizar.setContentDescription("Verificar atualizações do HX TV");
        atualizar.setOnClickListener(v->
                AppDataSourceManager.verificarAtualizacaoHx(getContext()));
        lateral.addView(atualizar,new LinearLayout.LayoutParams(-1,dp(42)));

        TextView antigo=texto("▤  MODO CLÁSSICO",11,SECUNDARIO,true);
        classicoNav=antigo;
        margem(antigo,11,10,3,10);
        foco(antigo);
        antigo.setOnClickListener(v->{
            if(getContext() instanceof BrowseActivity)
                ((BrowseActivity)getContext()).abrirModoClassico();
        });
        lateral.addView(antigo,new LinearLayout.LayoutParams(-1,dp(42)));
        for(TextView item:new TextView[]{pesquisa,atualizar,antigo}){
            item.setOnFocusChangeListener((v,ativo)->{
                v.setBackground(fundo(ativo?FOCO:PAINEL,ativo?0xFFEAF5FF:0,13));
                if(ativo)setPainelAberto(true);
            });
        }

        LinearLayout coluna=new LinearLayout(contexto);
        coluna.setOrientation(LinearLayout.VERTICAL);
        coluna.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF141B29,0xFF080C12,PRETO}));
        estrutura.addView(coluna,new LinearLayout.LayoutParams(0,-1,1));

        // A faixa superior é informativa. Os controles reais ficam na lateral,
        // longe da área utilizada pelo miniplayer flutuante do Android TV.
        LinearLayout topo=new LinearLayout(contexto);
        topo.setGravity(Gravity.CENTER_VERTICAL);
        margem(topo,24,10,23,8);
        coluna.addView(topo,new LinearLayout.LayoutParams(-1,dp(53)));
        TextView assinatura=texto("HX TV  /  SELEÇÃO PARA VOCÊ",13,TEXTO,true);
        assinatura.setLetterSpacing(0.10f);
        topo.addView(assinatura);
        topo.addView(new View(contexto),new LinearLayout.LayoutParams(0,1,1));
        TextView ambiente=texto("CINEMA  ·  SIGNATURE",10,GELO,true);
        ambiente.setLetterSpacing(0.1f);
        topo.addView(ambiente);

        View separador=new View(contexto);
        separador.setBackgroundColor(0xFF253344);
        coluna.addView(separador,new LinearLayout.LayoutParams(-1,dp(1)));

        rolagem=new ScrollView(contexto);
        rolagem.setFillViewport(false);
        rolagem.setVerticalScrollBarEnabled(false);
        rolagem.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        coluna.addView(rolagem,new LinearLayout.LayoutParams(-1,0,1));
        pagina=new LinearLayout(contexto);
        pagina.setOrientation(LinearLayout.VERTICAL);
        margem(pagina,24,14,24,30);
        rolagem.addView(pagina,new ScrollView.LayoutParams(-1,-2));
        TextView inicio=texto("ESCOLHA UMA CATEGORIA À ESQUERDA",15,SECUNDARIO,true);
        margem(inicio,0,24,0,20);
        pagina.addView(inicio);

        carregando=new ProgressBar(contexto);
        FrameLayout.LayoutParams progresso=new FrameLayout.LayoutParams(
                dp(42),dp(42),Gravity.CENTER);
        addView(carregando,progresso);
        carregando.setVisibility(View.GONE);
    }


    /** Os símbolos são tipográficos; o nome integral permanece acessível. */
    private String simbolo(String texto){
        String t=texto==null?"":texto.toLowerCase(java.util.Locale.ROOT);
        if(t.contains("iníc")||t.contains("inicio"))return "⌂";
        if(t.contains("esport"))return "◈";
        if(t.contains("hist"))return "↶";
        if(t.contains("vivo"))return "●";
        if(t.contains("jogo"))return "◇";
        if(t.contains("notíc")||t.contains("notic"))return "▣";
        if(t.contains("mús")||t.contains("mus"))return "♫";
        if(t.contains("canal")||t.contains("canais"))return "▥";
        if(t.contains("inscri"))return "☆";
        if(t.contains("playlist"))return "≡";
        if(t.contains("config"))return "⚙";
        return "•";
    }
    private void atualizarLegendasMenu(){
        for(int i=0;i<itensMenu.size()&&i<secoes.size();i++){
            TextView aba=itensMenu.get(i);
            String nome=secoes.get(i).getTitle();
            aba.setText(menuAberto?simbolo(nome)+"   "+nome:simbolo(nome));
            aba.setGravity(menuAberto?Gravity.CENTER_VERTICAL:Gravity.CENTER);
            margem(aba,menuAberto?13:1,8,menuAberto?7:1,8);
        }
        if(marcaNav!=null)marcaNav.setText(menuAberto?"HX  TV":"HX");
        if(subtituloNav!=null)subtituloNav.setVisibility(menuAberto?View.VISIBLE:View.GONE);
        if(alternarNav!=null)alternarNav.setText(menuAberto?"‹  RECOLHER MENU":"≡");
        if(pesquisaNav!=null)pesquisaNav.setText(menuAberto?"⌕  PESQUISAR":"⌕");
        if(atualizarNav!=null)atualizarNav.setText(menuAberto?"↻  ATUALIZAR":"↻");
        if(classicoNav!=null)classicoNav.setText(menuAberto?"▤  MODO CLÁSSICO":"▤");
        for(TextView item:new TextView[]{alternarNav,pesquisaNav,atualizarNav,classicoNav}){
            if(item!=null)item.setGravity(menuAberto?Gravity.CENTER_VERTICAL:Gravity.CENTER);
        }
    }
    private void setPainelAberto(boolean abrir){
        if(painelLateral==null||menuAberto==abrir)return;
        menuAberto=abrir;
        if(animacaoNav!=null)animacaoNav.cancel();
        atualizarLegendasMenu();
        int de=painelLateral.getLayoutParams().width;
        int ate=dp(abrir?174:68);
        animacaoNav=ValueAnimator.ofInt(de,ate);
        animacaoNav.setDuration(180);
        animacaoNav.addUpdateListener(a->{
            ViewGroup.LayoutParams p=painelLateral.getLayoutParams();
            p.width=(int)a.getAnimatedValue();
            painelLateral.setLayoutParams(p);
        });
        animacaoNav.start();
    }

    private void renovarMenu(){
        boolean restaurarMenu=menu.hasFocus();
        menu.removeAllViews();
        itensMenu.clear();
        for(int i=0;i<secoes.size();i++){
            final int indice=i;
            String nome=secoes.get(i).getTitle();
            TextView aba=texto(nome,13,TEXTO,true);
            aba.setContentDescription(nome);
            itensMenu.add(aba);
            aba.setSingleLine(true);
            aba.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            aba.setMarqueeRepeatLimit(-1);
            margem(aba,13,8,10,8);
            aba.setFocusable(true);
            aba.setOnFocusChangeListener((v,ativo)->{
                aba.setSelected(ativo);
                pintarCategoria(aba,ativo,secaoAtual==indice);
                if(ativo)setPainelAberto(true);
            });
            aba.setOnClickListener(v->abrirSecao(indice));
            // O DPAD chega primeiro ao item com foco. Capturar aqui evita que
            // o ScrollView lateral engula RIGHT no Início.
            aba.setOnKeyListener((v,key,event)->{
                if(event.getAction()==KeyEvent.ACTION_DOWN &&
                        key==KeyEvent.KEYCODE_DPAD_RIGHT){
                    if(secaoAtual!=indice)abrirSecao(indice);
                    return entrarNoConteudo();
                }
                return false;
            });
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(43));
            lp.bottomMargin=dp(5);
            menu.addView(aba,lp);
        }
        atualizarLegendasMenu();
        marcarMenu();
        if(restaurarMenu && menu.getChildCount()>0){
            final int restaurarIndice=Math.max(0,Math.min(secaoAtual,menu.getChildCount()-1));
            post(()->menu.getChildAt(restaurarIndice).requestFocus());
        }
    }
    /** Apenas o item com foco ganha CONTORNO. Categoria atual usa preenchimento discreto. */
    private void pintarCategoria(TextView aba, boolean focada, boolean atual){
        aba.setBackground(fundo(focada?FOCO:atual?0xFF29313E:0xFF181D26,
                focada?0xFFD7E7FF:0,10));
        aba.setTextColor(focada?TEXTO:atual?0xFFE0ECFA:SECUNDARIO);
    }
    private void marcarMenu(){
        for(int i=0;i<menu.getChildCount();i++){
            TextView aba=(TextView)menu.getChildAt(i);
            pintarCategoria(aba,aba.isFocused(),i==secaoAtual);
        }
    }

    private boolean estaNaPrimeiraFileira(View focado){
        View atual=focado;
        while(atual!=null){
            if(primeiraFileira.contains(atual))return true;
            if(!(atual.getParent() instanceof View))break;
            atual=(View)atual.getParent();
        }
        return false;
    }

    private boolean focarCategoriaAtual(){
        if(menu.getChildCount()==0)return false;
        int indice=Math.max(0,Math.min(secaoAtual,menu.getChildCount()-1));
        setPainelAberto(true);
        View aba=menu.getChildAt(indice);
        if(aba.requestFocus()){
            estavaNoMenu=true;
            ultimoControleFocado=aba;
            marcarMenu();
            return true;
        }
        return false;
    }

    // Foco explícito: LATERAL ↔ CARDS. No Android TV, deixar a busca
    // automática decidir esse salto faz o cursor sumir atrás do miniplayer.
    private boolean primeiraColunaDaFaixa(View focado){
        if(focado==null)return false;
        for(LinearLayout faixa:fileiras.values()){
            if(faixa.getChildCount()>0 && faixa.getChildAt(0)==focado)return true;
        }
        return focado==primeiroCartao && primeiroCartao!=null;
    }

    /** Localiza sempre um card conectado. Não confia em referência antiga. */
    private View primeiroCardValido(){
        for(LinearLayout faixa:fileiras.values()){
            for(int i=0;i<faixa.getChildCount();i++){
                View card=faixa.getChildAt(i);
                if(card!=null&&card.isAttachedToWindow()&&card.isShown()
                        &&card.isFocusable())return card;
            }
        }
        return null;
    }

    private boolean entrarNosCards(){
        View card=primeiroCardValido();
        if(card!=null&&card.requestFocus()){
            primeiroCartao=card;
            entrarNoConteudoPendente=false;
            return true;
        }
        entrarNoConteudoPendente=true;
        return true;
    }

    private boolean entrarNoConteudo(){
        // Toda a navegação da esquerda começa por um destino visual seguro.
        // Não usa ultimoControleFocado: depois da troca de seção pode ser
        // uma referência inválida da tela anterior.
        View alvo=destaqueFocavel;
        if(alvo==null || !alvo.isAttachedToWindow() || !alvo.isShown())
            alvo=primeiroCardValido();
        if(alvo!=null && alvo.requestFocus()){
            entrarNoConteudoPendente=false;
            return true;
        }
        entrarNoConteudoPendente=true;
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent evento){
        if(evento.getAction()==KeyEvent.ACTION_DOWN){
            View atual=findFocus();
            int tecla=evento.getKeyCode();
            if(tecla==KeyEvent.KEYCODE_DPAD_RIGHT && menu.hasFocus()){
                return entrarNoConteudo();
            }
            if(tecla==KeyEvent.KEYCODE_DPAD_LEFT &&
                    atual!=null && primeiraColunaDaFaixa(atual)){
                if(focarCategoriaAtual())return true;
            }
            if(tecla==KeyEvent.KEYCODE_DPAD_UP &&
                    atual!=null && estaNaPrimeiraFileira(atual)){
                if(focarCategoriaAtual())return true;
            }
        }
        return super.dispatchKeyEvent(evento);
    }
    private void limparConteudo(){
        pagina.removeAllViews();
        fileiras.clear();
        grupos.clear();
        primeiroCartao=null;
        destaqueFocavel=null;
        grupoPrincipalId=null;
        primeiraFileira.clear();
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
            entrarNoConteudoPendente=false;
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
        vertical.bottomMargin=dp(21);
        pagina.addView(exterior,0,vertical);

        TextView rotulo=texto("HX TV ORIGINAL  •  SELEÇÃO DO DIA",11,GELO,true);
        rotulo.setLetterSpacing(0.15f);
        margem(rotulo,1,0,0,10);
        exterior.addView(rotulo);

        // Hero cinematográfico: uma imagem única com vinheta lateral,
        // e textos em área protegida. Não é um segundo player nem autoplay.
        FrameLayout quadro=new FrameLayout(getContext());
        quadro.setBackground(fundo(0xFF101721,0,17));
        if(Build.VERSION.SDK_INT>=21)quadro.setClipToOutline(true);
        exterior.addView(quadro,new LinearLayout.LayoutParams(-1,dp(240)));
        destaqueFocavel=quadro;
        quadro.setFocusable(true);
        quadro.setClickable(true);
        quadro.setOnClickListener(v->{
            if(videoDestaque!=null){
                guardarFocoAntesDeSair();
                dados.onVideoItemClicked(videoDestaque);
            }
        });
        quadro.setOnFocusChangeListener((v,active)->{
            quadro.setForeground(fundo(0x00000000,active?0xFFE6F1FF:0,17));
            if(Build.VERSION.SDK_INT>=21)quadro.setElevation(dp(active?8:0));
            v.animate().cancel();
            v.animate().scaleX(active?1.01f:1f).scaleY(active?1.01f:1f)
                    .setDuration(165).start();
            if(active)setPainelAberto(false);
        });
        quadro.setOnKeyListener((v,key,event)->{
            if(event.getAction()==KeyEvent.ACTION_DOWN &&
                    key==KeyEvent.KEYCODE_DPAD_DOWN)return entrarNosCards();
            if(event.getAction()==KeyEvent.ACTION_DOWN &&
                    key==KeyEvent.KEYCODE_DPAD_LEFT)return focarCategoriaAtual();
            return false;
        });

        imagemDestaque=new ImageView(getContext());
        imagemDestaque.setScaleType(ImageView.ScaleType.CENTER_CROP);
        quadro.addView(imagemDestaque,new FrameLayout.LayoutParams(-1,-1));

        View vinheta=new View(getContext());
        vinheta.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF060A12,0xFC080D17,0xDD0B1423,0x220D1622}));
        quadro.addView(vinheta,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout lateralTexto=new LinearLayout(getContext());
        lateralTexto.setOrientation(LinearLayout.VERTICAL);
        lateralTexto.setGravity(Gravity.CENTER_VERTICAL);
        margem(lateralTexto,27,14,12,14);
        LinearLayout area=new LinearLayout(getContext());
        area.setOrientation(LinearLayout.HORIZONTAL);
        quadro.addView(area,new FrameLayout.LayoutParams(-1,-1));
        area.addView(lateralTexto,new LinearLayout.LayoutParams(0,-1,58));
        area.addView(new View(getContext()),new LinearLayout.LayoutParams(0,-1,42));

        seloDestaque=texto("EM DESTAQUE",11,GELO,true);
        seloDestaque.setLetterSpacing(0.13f);
        lateralTexto.addView(seloDestaque);

        tituloDestaque=texto("",27,TEXTO,true);
        tituloDestaque.setMaxLines(3);
        tituloDestaque.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.topMargin=dp(11);
        lateralTexto.addView(tituloDestaque,lp);

        detalhesDestaque=texto("",12,SECUNDARIO,false);
        detalhesDestaque.setMaxLines(2);
        detalhesDestaque.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,-2);
        mp.topMargin=dp(11);
        lateralTexto.addView(detalhesDestaque,mp);

        TextView indicacao=texto("▶  ASSISTIR AGORA   •   OK",12,TEXTO,true);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);
        hp.topMargin=dp(15);
        lateralTexto.addView(indicacao,hp);

        atualizarDestaque(primeiro);
    }
    private void atualizarDestaque(Video video){
        if(video==null||tituloDestaque==null)return;
        videoDestaque=video;
        tituloDestaque.setText(video.getTitle());
        detalhesDestaque.setText(video.getSecondTitle());
        seloDestaque.setText(video.isLive?"●  AO VIVO":"EM DESTAQUE");
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
            TextView cabecalho=texto(principal?"RECOMENDADO PARA VOCÊ":nome,18,TEXTO,true);
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
        // O catálogo pode atualizar enquanto o cursor está numa miniatura.
        // A view antiga será removida; agendar foco para a nova view equivalente.
        if(faixa.hasFocus()){
            focoPendente=true;
            ultimaSecaoFocada=secaoAtual;
        }
        if(principal)primeiraFileira.clear();
        final LinearLayout faixaAtual=faixa;
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
                    dp(224),dp(187));
            cp.rightMargin=dp(12);faixa.addView(cartao,cp);
            // Selo sobre a miniatura verdadeira, sem mascarar a interface.
            FrameLayout capa=new FrameLayout(getContext());
            cartao.addView(capa,new LinearLayout.LayoutParams(-1,
                    dp(126)));
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
            if(principal)primeiraFileira.add(cartao);
            cartao.setOnFocusChangeListener((v,selecionado)->{
                if(selecionado)setPainelAberto(false);
                v.setBackground(fundo(selecionado?0xFF22364F:PAINEL,
                        selecionado?0xFFE4F3FF:0,14));
                v.animate().cancel();
                v.animate().scaleX(selecionado?1.037f:1f)
                        .scaleY(selecionado?1.037f:1f)
                        .translationY(selecionado?-dp(3):0)
                        .setDuration(180).start();
                if(Build.VERSION.SDK_INT>=21)
                    v.setElevation(dp(selecionado?6:0));
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
            // Rotas explícitas na grade; não dependem de FocusFinder atravessar
            // múltiplos HorizontalScrollViews em diferentes versões Android.
            cartao.setOnKeyListener((v,key,event)->{
                if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;
                if(key==KeyEvent.KEYCODE_DPAD_LEFT && pos==0)
                    return focarCategoriaAtual();
                if(key==KeyEvent.KEYCODE_DPAD_UP && principal)
                    return destaqueFocavel!=null
                            && destaqueFocavel.requestFocus();
                if(key==KeyEvent.KEYCODE_DPAD_DOWN
                        || (key==KeyEvent.KEYCODE_DPAD_UP && !principal)){
                    List<LinearLayout> linhas=new ArrayList<>(fileiras.values());
                    int atualLinha=linhas.indexOf(faixaAtual);
                    int destino=atualLinha+(key==KeyEvent.KEYCODE_DPAD_DOWN?1:-1);
                    if(destino>=0 && destino<linhas.size()){
                        LinearLayout outra=linhas.get(destino);
                        if(outra.getChildCount()>0){
                            View proximo=outra.getChildAt(
                                    Math.min(pos,outra.getChildCount()-1));
                            if(proximo!=null&&proximo.requestFocus())return true;
                        }
                    }
                }
                return false;
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
        if(entrarNoConteudoPendente && (primeiroCartao!=null || destaqueFocavel!=null)){
            post(this::entrarNoConteudo);
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
        // A primeira lista com vídeos REAIS vira o destaque. Grupos vazios
        // podem chegar antes dos vídeos e não podem consumir o slot principal.
        if(grupoPrincipalId==null)grupoPrincipalId=id;
        boolean principal=id.equals(grupoPrincipalId);
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
        TextView atualizar=texto("↻   ATUALIZAR HX TV",17,GELO,true);
        margem(atualizar,18,12,18,12);foco(atualizar);
        LinearLayout.LayoutParams linhaAtualizacao=new LinearLayout.LayoutParams(-1,dp(58));
        linhaAtualizacao.bottomMargin=dp(10);
        pagina.addView(atualizar,linhaAtualizacao);
        atualizar.setOnClickListener(v->
                AppDataSourceManager.verificarAtualizacaoHx(getContext()));
        primeiroCartao=atualizar;
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
        if(entrarNoConteudoPendente)post(this::entrarNoConteudo);
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
            if(focoConteudo){
                entrarNoConteudoPendente=true;
                postDelayed(this::entrarNoConteudo,250);
            }else{
                postDelayed(()->{if(!hasFocus())focarCategoriaAtual();},150);
            }
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
        executar(this::entrarNoConteudo);
    }
    @Override public void showError(ErrorFragmentData erro){
        executar(()->{
            limparConteudo();
            TextView msg=texto(erro==null?"Não foi possível carregar este conteúdo":
                    erro.getMessage(),17,TEXTO,true);
            margem(msg,5,25,0,20);pagina.addView(msg);
            if(erro!=null && erro.getActionText()!=null){
                TextView tentar=texto(erro.getActionText(),15,GELO,true);
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

    /** Voltar, quando dentro dos vídeos, retorna à navegação lateral. */
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
