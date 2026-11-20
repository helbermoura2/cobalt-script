package com.liskovsoft.smartyoutubetv2.tv.ui.browse;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
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
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.mediaserviceinterfaces.data.SearchOptions;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;
import io.reactivex.disposables.Disposable;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Tela inicial independente HX TV. Sem BrowseFragment / WebView / máscara.
 * Compartilha apenas os dados, contas e a reprodução do SmartTube.
 */
public final class HxHomeScreen extends FrameLayout implements BrowseView {
    private static final int PRETO=0xFF08090D, PAINEL=0xFF181B23;
    private static final int GELO=0xFFD5E0EB, TEXTO=0xFFF7F8FA, SECUNDARIO=0xFF9DA6B2;
    private static final int FOCO=0xFF313C4D;
    private final BrowsePresenter dados;
    private final List<BrowseSection> secoes=new ArrayList<>();
    private final LinkedHashMap<String,LinearLayout> fileiras=new LinkedHashMap<>();
    private final LinkedHashMap<String,List<Video>> grupos=new LinkedHashMap<>();
    private final LinkedHashMap<String,Integer> limitesPorFileira=new LinkedHashMap<>();
    // Registro da home: não repetir o mesmo vídeo em várias fileiras recomendadas.
    private final LinkedHashMap<String,LinkedHashSet<String>> idsNaTela=new LinkedHashMap<>();
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
    // HOME Horizon: cards estáveis 16:9, foco rápido sem reflow de layout.
    private static final int LARGURA_CAPA_HOME=205;
    private static final int ALTURA_CAPA_HOME=164;
    private final List<Disposable> buscasAssuntos=new ArrayList<>();
    private int geracaoPagina=0, proximoAssunto=0;
    private boolean exploracaoPronta=false, hubAoVivoPronto=false;
    private final LinkedHashMap<String,Video> transmissões=new LinkedHashMap<>();
    private final LinkedHashMap<Integer,LinkedHashMap<String,Video>> livesPorTema=new LinkedHashMap<>();
    private final List<TextView> filtrosDeLive=new ArrayList<>();
    private TextView statusDeLive;
    private int filtroLiveSelecionado=-1;
    private static final String[][] LIVE_ASSUNTOS={
        {"Notícias","jornal notícias brasil ao vivo agora"},
        {"Esportes","esportes futebol jogos ao vivo hoje"},
        {"Games","live games gameplay streaming ao vivo agora"},
        {"Música","rádio música show ao vivo transmissão"},
        {"Podcasts","podcast entrevista ao vivo neste momento"},
        {"Natureza","câmera ao vivo natureza animais"},
        {"Tecnologia","tecnologia eventos ao vivo transmissão"},
        {"Mundo","live news world streaming now"}
    };
    private static final int MAX_VIDEOS_POR_FILEIRA=56; // compatibilidade do catálogo; crescimento é progressivo
    // Fontes reais: cada assunto pesquisa pelo próprio serviço de conteúdo
    // do aplicativo. Resultados, capas e autoria vêm da fonte, nunca são gerados.
    private static final String[][] ASSUNTOS={
        {"Cinema e trailers","trailers filmes cinema lançamentos brasil"},
        {"Séries e estreias","trailer séries estreias novidades"},
        {"Tecnologia","tecnologia novidades análises dispositivos"},
        {"Inteligência artificial","inteligência artificial novidades ferramentas"},
        {"Ciência","ciência descobertas pesquisas"},
        {"Astronomia e espaço","astronomia NASA universo descobertas"},
        {"Documentários","documentário completo história natureza"},
        {"História","história curiosidades civilizações documentário"},
        {"Futebol","futebol melhores momentos campeonato brasileiro"},
        {"Esportes","esportes melhores momentos notícias"},
        {"Música","música lançamentos videoclipes brasileiros"},
        {"Shows ao vivo","show ao vivo música apresentações"},
        {"Games","jogos gameplay lançamentos 2026"},
        {"Humor","comédia humor vídeos engraçados"},
        {"Natureza","natureza animais vida selvagem documentário"},
        {"Viagens","viagens destinos passeios mundo"},
        {"Culinária","receitas gastronomia cozinha"},
        {"Automóveis","carros testes lançamentos automobilismo"},
        {"Finanças","economia mercado finanças atualidades"},
        {"Notícias","notícias brasil mundo hoje"},
        {"Podcasts","podcasts entrevistas brasil"},
        {"Educação","aprender aulas conhecimento"},
        {"Programação","programação desenvolvimento software tutoriais"},
        {"Arquitetura e design","arquitetura interiores design casas"},
        {"Saúde e bem-estar","saúde atividade física bem estar"},
        {"Animações e anime","anime animação trailers novidades"},
        {"Infantil e família","vídeos educativos para crianças família"},
        {"Ao vivo agora","ao vivo transmissões brasil"}
    };

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
                new int[]{0xFF1B2029,0xFF12171E,0xFF0B1017}));
        margem(lateral,11,20,11,14);
        painelLateral=lateral;
        estrutura.addView(lateral,new LinearLayout.LayoutParams(dp(166),-1));

        TextView marca=texto("HX  TV",26,TEXTO,true);
        marcaNav=marca;
        marca.setLetterSpacing(0.06f);
        margem(marca,9,0,0,0);
        lateral.addView(marca);
        TextView lema=texto("A SUA EXPERIÊNCIA",9,SECUNDARIO,true);
        subtituloNav=lema;
        lema.setLetterSpacing(0.12f);
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
        linhaBronze.setBackgroundColor(0xFF2C333E);
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
                new int[]{0xFF101822,0xFF080D14,PRETO}));
        estrutura.addView(coluna,new LinearLayout.LayoutParams(0,-1,1));

        // A faixa superior é informativa. Os controles reais ficam na lateral,
        // longe da área utilizada pelo miniplayer flutuante do Android TV.
        LinearLayout topo=new LinearLayout(contexto);
        topo.setGravity(Gravity.CENTER_VERTICAL);
        margem(topo,22,7,18,5);
        coluna.addView(topo,new LinearLayout.LayoutParams(-1,dp(47)));
        TextView assinatura=texto("DESCUBRA",14,TEXTO,true);
        assinatura.setLetterSpacing(0.08f);
        topo.addView(assinatura);
        topo.addView(new View(contexto),new LinearLayout.LayoutParams(0,1,1));
        TextView ambiente=texto("",10,SECUNDARIO,true);
        ambiente.setLetterSpacing(0.1f);
        topo.addView(ambiente);

        View separador=new View(contexto);
        separador.setBackgroundColor(0xFF27313D);
        coluna.addView(separador,new LinearLayout.LayoutParams(-1,dp(1)));

        rolagem=new ScrollView(contexto);
        rolagem.setFillViewport(false);
        rolagem.setVerticalScrollBarEnabled(false);
        rolagem.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        coluna.addView(rolagem,new LinearLayout.LayoutParams(-1,0,1));
        pagina=new LinearLayout(contexto);
        pagina.setOrientation(LinearLayout.VERTICAL);
        margem(pagina,20,12,20,30);
        rolagem.addView(pagina,new ScrollView.LayoutParams(-1,-2));
        TextView inicio=texto("ESCOLHA UMA CATEGORIA À ESQUERDA",15,SECUNDARIO,true);
        margem(inicio,0,24,0,20);
        pagina.addView(inicio);

        carregando=new ProgressBar(contexto);
        FrameLayout.LayoutParams progresso=new FrameLayout.LayoutParams(
                dp(42),dp(42),Gravity.CENTER);
        addView(carregando,progresso);
        carregando.setVisibility(View.GONE);
        // O menu já nasce recolhido; o foco nele abre os nomes automaticamente.
        post(()->{if(!menu.hasFocus())setPainelAberto(false);});
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
            boolean temIcone=secoes.get(i).getResId()>0
                    && aba.getCompoundDrawables()[0]!=null;
            aba.setText(temIcone?(menuAberto?nome:""):
                    (menuAberto?simbolo(nome)+"  "+nome:simbolo(nome)));
            aba.setGravity(menuAberto?Gravity.CENTER_VERTICAL:Gravity.CENTER);
            margem(aba,menuAberto?12:1,8,menuAberto?5:1,8);
        }
        if(marcaNav!=null){
            marcaNav.setText(menuAberto?"HX  TV":"HX");
            marcaNav.setTextSize(menuAberto?26:21);
            margem(marcaNav,menuAberto?9:0,0,0,0);
        }
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
        int ate=dp(abrir?166:64);
        animacaoNav=ValueAnimator.ofInt(de,ate);
        animacaoNav.setDuration(150);
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
            TextView aba=texto(nome,12,TEXTO,true);
            aba.setContentDescription(nome);
            int recurso=secoes.get(i).getResId();
            if(recurso>0){
                try{
                    Drawable icone=getResources().getDrawable(recurso).mutate();
                    icone.setBounds(0,0,dp(18),dp(18));
                    icone.setTint(SECUNDARIO);
                    aba.setCompoundDrawables(icone,null,null,null);
                    aba.setCompoundDrawablePadding(dp(9));
                }catch(android.content.res.Resources.NotFoundException ignorado){}
            }
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
        aba.setTextColor(focada?TEXTO:atual?0xFFE2EAF3:SECUNDARIO);
        Drawable icone=aba.getCompoundDrawables()[0];
        if(icone!=null)icone.setTint(focada?TEXTO:atual?GELO:SECUNDARIO);
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
            if(!estaNaHome() && tecla==KeyEvent.KEYCODE_DPAD_UP &&
                    atual!=null && estaNaPrimeiraFileira(atual)){
                if(focarCategoriaAtual())return true;
            }
        }
        return super.dispatchKeyEvent(evento);
    }

    private void limparConteudo(){
        geracaoPagina++;
        for(Disposable d:buscasAssuntos)if(d!=null&&!d.isDisposed())d.dispose();
        buscasAssuntos.clear();
        exploracaoPronta=false;
        hubAoVivoPronto=false;
        transmissões.clear();
        livesPorTema.clear();
        filtrosDeLive.clear();
        statusDeLive=null;
        filtroLiveSelecionado=-1;
        proximoAssunto=0;
        pagina.removeAllViews();
        fileiras.clear();
        grupos.clear();
        limitesPorFileira.clear();
        idsNaTela.clear();
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
    private boolean estaNaHome(){
        return secaoAtual>=0 && secaoAtual<secoes.size()
                && secoes.get(secaoAtual).getId()==MediaGroup.TYPE_HOME;
    }
    private boolean estaNaAreaAoVivo(){
        return secaoAtual>=0 && secaoAtual<secoes.size()
                && secoes.get(secaoAtual).getId()==MediaGroup.TYPE_LIVE;
    }
    private boolean podeExibirHubAoVivo(){
        return estaNaHome()||estaNaAreaAoVivo();
    }
    // Pesquisa transversal por vários temas, para não depender de um único
    // grupo de transmissões ao vivo sugerido pelo algoritmo.
    private void adicionarHubAoVivo(){
        if(hubAoVivoPronto||!podeExibirHubAoVivo()||videoDestaque==null)return;
        hubAoVivoPronto=true;
        LinearLayout area=new LinearLayout(getContext());
        area.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.bottomMargin=dp(10);
        pagina.addView(area,Math.min(2,pagina.getChildCount()),lp);
        TextView titulo=texto("●  AO VIVO AGORA",14,TEXTO,true);
        area.addView(titulo);
        statusDeLive=texto("Buscando transmissões disponíveis...",10,SECUNDARIO,false);
        LinearLayout.LayoutParams t=new LinearLayout.LayoutParams(-1,-2);
        t.topMargin=dp(2);t.bottomMargin=dp(9);
        area.addView(statusDeLive,t);
        HorizontalScrollView scroll=new HorizontalScrollView(getContext());
        scroll.setHorizontalScrollBarEnabled(false);
        area.addView(scroll);
        LinearLayout trilho=new LinearLayout(getContext());
        trilho.setOrientation(LinearLayout.HORIZONTAL);
        scroll.addView(trilho,new HorizontalScrollView.LayoutParams(-2,-2));
        fileiras.put("hx/hub-ao-vivo",trilho);
        for(int i=-1;i<LIVE_ASSUNTOS.length;i++){
            final int indice=i;
            String nome=i<0?"Todos":LIVE_ASSUNTOS[i][0];
            TextView chip=texto(nome,12,TEXTO,true);
            chip.setGravity(Gravity.CENTER);
            chip.setContentDescription("Filtrar transmissões: "+nome);
            foco(chip);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(i<0?76:104),dp(37));
            p.rightMargin=dp(7);
            trilho.addView(chip,p);
            filtrosDeLive.add(chip);
            chip.setOnFocusChangeListener((v,ativo)->{
                pintarFiltroLive(chip,ativo,indice==filtroLiveSelecionado);
                if(ativo)setPainelAberto(false);
            });
            chip.setOnClickListener(v->{
                filtroLiveSelecionado=indice;
                for(int j=0;j<filtrosDeLive.size();j++)
                    pintarFiltroLive(filtrosDeLive.get(j),
                            filtrosDeLive.get(j).isFocused(),j-1==indice);
                atualizarFileiraLive();
            });
            pintarFiltroLive(chip,false,i==-1);
        }
        final int geracao=geracaoPagina;
        for(int i=0;i<LIVE_ASSUNTOS.length;i++){
            final int indice=i;
            postDelayed(()->pesquisarLive(indice,geracao),300L+(long)i*650L);
        }
    }
    private void pintarFiltroLive(TextView chip,boolean emFoco,boolean selecionado){
        chip.setBackground(fundo(emFoco?0xFF47566B:
                selecionado?0xFF2D3848:0xFF19232E,
                emFoco?0xFFF4F7FC:0,11));
        chip.setTextColor(emFoco?TEXTO:selecionado?GELO:SECUNDARIO);
    }
    private void atualizarFileiraLive(){
        if(!podeExibirHubAoVivo()||statusDeLive==null)return;
        List<Video> lista=new ArrayList<>();
        if(filtroLiveSelecionado<0)lista.addAll(transmissões.values());
        else{
            LinkedHashMap<String,Video> selecionadas=livesPorTema.get(filtroLiveSelecionado);
            if(selecionadas!=null)lista.addAll(selecionadas.values());
        }
        String rotulo=filtroLiveSelecionado<0?"Todos":LIVE_ASSUNTOS[filtroLiveSelecionado][0];
        if(lista.isEmpty()){
            statusDeLive.setText("Nenhuma live encontrada em "+rotulo+". Escolha outro tema.");
            LinearLayout anterior=fileiras.get("hx/live/multitemas");
            if(anterior!=null&&!anterior.hasFocus())anterior.removeAllViews();
            return;
        }
        statusDeLive.setText(lista.size()+" transmissões encontradas · "+rotulo);
        desenharFileira("hx/live/multitemas","ASSISTA AO VIVO",lista,false);
    }
    private void pesquisarLive(int indice,int geracao){
        if(geracao!=geracaoPagina||!podeExibirHubAoVivo()||!isAttachedToWindow())return;
        final String consulta=LIVE_ASSUNTOS[indice][1];
        try{
            Disposable busca=YouTubeServiceManager.instance().getContentService()
                .getSearchObserve(consulta,
                    SearchOptions.FEATURE_LIVE | SearchOptions.TYPE_VIDEO)
                .subscribe(resultado->{
                    if(resultado==null)return;
                    List<Video> validos=new ArrayList<>();
                    for(MediaGroup grupo:resultado){
                        if(grupo==null||grupo.isEmpty())continue;
                        VideoGroup videos=VideoGroup.from(grupo);
                        if(videos.getVideos()==null)continue;
                        for(Video video:videos.getVideos()){
                            if(video!=null&&video.isLive&&!video.isLiveEnd
                                    &&!video.isUpcoming&&video.videoId!=null)
                                validos.add(video);
                        }
                    }
                    if(validos.isEmpty())return;
                    post(()->{
                        if(geracao!=geracaoPagina||!podeExibirHubAoVivo())return;
                        LinkedHashMap<String,Video> porTema=livesPorTema.get(indice);
                        if(porTema==null){
                            porTema=new LinkedHashMap<>();
                            livesPorTema.put(indice,porTema);
                        }
                        for(Video v:validos){
                            if(porTema.size()<28)porTema.put(v.videoId,v);
                            if(transmissões.size()<64)transmissões.put(v.videoId,v);
                        }
                        atualizarFileiraLive();
                    });
                },erro->android.util.Log.w("HX-Live",
                        "Falha isolada em "+LIVE_ASSUNTOS[indice][0],erro));
            buscasAssuntos.add(busca);
        }catch(RuntimeException e){
            android.util.Log.w("HX-Live","Pesquisa indisponível",e);
        }
    }

    private void adicionarExplorar(){
        if(exploracaoPronta||!estaNaHome()||videoDestaque==null)return;
        exploracaoPronta=true;
        LinearLayout painel=new LinearLayout(getContext());
        painel.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams bloco=new LinearLayout.LayoutParams(-1,-2);
        bloco.topMargin=dp(2);bloco.bottomMargin=dp(12);
        pagina.addView(painel,Math.min(4,pagina.getChildCount()),bloco);
        TextView titulo=texto("EXPLORE SEUS INTERESSES",14,TEXTO,true);
        titulo.setLetterSpacing(0.04f);
        painel.addView(titulo);
        TextView legenda=texto(ASSUNTOS.length+" assuntos, vídeos reais e novidades",10,SECUNDARIO,false);
        LinearLayout.LayoutParams sub=new LinearLayout.LayoutParams(-1,-2);
        sub.topMargin=dp(4);sub.bottomMargin=dp(12);
        painel.addView(legenda,sub);
        HorizontalScrollView deslocamento=new HorizontalScrollView(getContext());
        deslocamento.setHorizontalScrollBarEnabled(false);
        painel.addView(deslocamento);
        LinearLayout linha=new LinearLayout(getContext());
        linha.setOrientation(LinearLayout.HORIZONTAL);
        deslocamento.addView(linha,new HorizontalScrollView.LayoutParams(-2,-2));
        fileiras.put("hx/explorar",linha);
        for(String[] assunto:ASSUNTOS){
            TextView chip=texto("⌕  "+assunto[0],13,TEXTO,true);
            chip.setContentDescription("Explorar: "+assunto[0]);
            chip.setSingleLine(true);
            margem(chip,16,9,16,9);
            foco(chip);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,dp(38));
            cp.rightMargin=dp(9);
            linha.addView(chip,cp);
            chip.setOnFocusChangeListener((v,selecionado)->{
                chip.setBackground(fundo(selecionado?0xFF324A69:0xFF1B2533,
                        selecionado?0xFFF1F7FF:0,12));
                if(selecionado)setPainelAberto(false);
            });
            chip.setOnClickListener(v->
                    SearchPresenter.instance(getContext()).startSearch(assunto[1]));
        }
        carregarProximosAssuntos(4);
    }
    private void carregarProximosAssuntos(int quantidade){
        if(!estaNaHome()||!exploracaoPronta)return;
        final int inicio=proximoAssunto;
        final int ate=Math.min(ASSUNTOS.length,inicio+quantidade);
        if(inicio>=ate)return;
        proximoAssunto=ate;
        final int geracao=geracaoPagina;
        for(int i=inicio;i<ate;i++){
            final int assuntoIndex=i;
            postDelayed(()->pesquisarAssunto(assuntoIndex,geracao),
                    (long)(i-inicio)*550L);
        }
    }
    private void pesquisarAssunto(int index,int geracao){
        if(!isAttachedToWindow() || geracao!=geracaoPagina || !estaNaHome())return;
        final String titulo=ASSUNTOS[index][0], consulta=ASSUNTOS[index][1];
        try{
            Disposable busca=YouTubeServiceManager.instance()
                .getContentService().getSearchObserve(consulta,0)
                .subscribe(resultados->{
                    if(resultados==null)return;
                    for(MediaGroup resultado:resultados){
                        if(resultado==null||resultado.isEmpty())continue;
                        VideoGroup grupo=VideoGroup.from(resultado);
                        List<Video> videos=grupo.getVideos();
                        if(videos==null||videos.isEmpty())continue;
                        post(()->{
                            if(geracao!=geracaoPagina||!estaNaHome())return;
                            String chave="hx/tema/"+index;
                            if(fileiras.containsKey(chave))return;
                            desenharFileira(chave,titulo,videos,false);
                        });
                        break;
                    }
                },erro->{
                    // Falha isolada por tema: nunca interrompe a home.
                    android.util.Log.w("HX-Discover",titulo+": "+erro);
                });
            buscasAssuntos.add(busca);
        }catch(RuntimeException e){
            android.util.Log.w("HX-Discover","Busca indisponível: "+titulo,e);
        }
    }
    private String chave(VideoGroup g){
        return g.getId()+"/"+(g.getTitle()==null?"Vídeos":g.getTitle());
    }
    private void destaque(Video primeiro){
        LinearLayout exterior=new LinearLayout(getContext());
        exterior.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams vertical=new LinearLayout.LayoutParams(-1,-2);
        vertical.bottomMargin=dp(11);
        pagina.addView(exterior,0,vertical);

        TextView rotulo=texto("ESCOLHIDO PARA VOCÊ",10,GELO,true);
        rotulo.setLetterSpacing(0.15f);
        margem(rotulo,1,0,0,6);
        exterior.addView(rotulo);

        // Hero cinematográfico: uma imagem única com vinheta lateral,
        // e textos em área protegida. Não é um segundo player nem autoplay.
        FrameLayout quadro=new FrameLayout(getContext());
        quadro.setBackground(fundo(0xFF101721,0,17));
        if(Build.VERSION.SDK_INT>=21)quadro.setClipToOutline(true);
        exterior.addView(quadro,new LinearLayout.LayoutParams(-1,dp(142)));
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
        margem(lateralTexto,20,9,10,9);
        LinearLayout area=new LinearLayout(getContext());
        area.setOrientation(LinearLayout.HORIZONTAL);
        quadro.addView(area,new FrameLayout.LayoutParams(-1,-1));
        area.addView(lateralTexto,new LinearLayout.LayoutParams(0,-1,58));
        area.addView(new View(getContext()),new LinearLayout.LayoutParams(0,-1,42));

        seloDestaque=texto("HX TV",10,GELO,true);
        seloDestaque.setLetterSpacing(0.13f);
        lateralTexto.addView(seloDestaque);

        tituloDestaque=texto("",19,TEXTO,true);
        tituloDestaque.setMaxLines(2);
        tituloDestaque.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.topMargin=dp(6);
        lateralTexto.addView(tituloDestaque,lp);

        detalhesDestaque=texto("",10,SECUNDARIO,false);
        detalhesDestaque.setMaxLines(1);
        detalhesDestaque.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,-2);
        mp.topMargin=dp(5);
        lateralTexto.addView(detalhesDestaque,mp);

        TextView indicacao=texto("▶  ASSISTIR",11,0xFF101928,true);
        indicacao.setGravity(Gravity.CENTER);
        indicacao.setBackground(fundo(0xEDEDF5FF,0,8));
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(dp(130),dp(30));
        hp.topMargin=dp(8);
        lateralTexto.addView(indicacao,hp);

        atualizarDestaque(primeiro);
    }
    private void atualizarDestaque(Video video){
        if(video==null||tituloDestaque==null)return;
        videoDestaque=video;
        tituloDestaque.setText(video.getTitle());
        detalhesDestaque.setText(video.getSecondTitle());
        seloDestaque.setText(video.isLive?"●  AO VIVO":"HX TV  ·  DESTAQUE");
        Glide.with(getContext())
             .load(ClickbaitRemover.updateThumbnail(video,
                     MainUIData.instance(getContext()).getThumbQuality()))
             .diskCacheStrategy(DiskCacheStrategy.ALL)
              .transition(DrawableTransitionOptions.withCrossFade(240))
             .error(R.drawable.card_placeholder).into(imagemDestaque);
    }
    // Uma única aparição por vídeo entre fileiras da Home.
    // Transmissões no hub são exceção: a prioridade é localizar lives ativas.
    private List<Video> semDuplicarNaHome(String chave,List<Video> candidatos){
        if(!estaNaHome()||chave.startsWith("hx/live/"))return candidatos;
        LinkedHashSet<String> utilizados=new LinkedHashSet<>();
        for(Map.Entry<String,LinkedHashSet<String>> registro:idsNaTela.entrySet()){
            if(!registro.getKey().equals(chave))utilizados.addAll(registro.getValue());
        }
        LinkedHashSet<String> atuais=new LinkedHashSet<>();
        List<Video> distintos=new ArrayList<>();
        for(Video video:candidatos){
            if(video==null)continue;
            String id=video.videoId;
            if(id==null||(!utilizados.contains(id)&&atuais.add(id)))
                distintos.add(video);
        }
        idsNaTela.put(chave,atuais);
        return distintos;
    }
    private void desenharFileira(String chave,String nome,List<Video> videos,boolean principal){
        final List<Video> exibidos=semDuplicarNaHome(chave,videos);
        if(exibidos==null||exibidos.isEmpty())return;
        LinearLayout faixa=fileiras.get(chave);
        if(faixa==null){
            LinearLayout bloco=new LinearLayout(getContext());
            bloco.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);
            bp.bottomMargin=dp(17);
            if(chave.startsWith("hx/live/")&&podeExibirHubAoVivo())
                pagina.addView(bloco,Math.min(3,pagina.getChildCount()),bp);
            else pagina.addView(bloco,bp);
            TextView cabecalho=texto(principal?"PARA VOCÊ":nome,15,TEXTO,true);
            margem(cabecalho,1,0,0,9);bloco.addView(cabecalho);
            HorizontalScrollView deslizar=new HorizontalScrollView(getContext());
            deslizar.setHorizontalScrollBarEnabled(false);
            deslizar.setClipToPadding(false);
            deslizar.setClipChildren(false);
            bloco.addView(deslizar);
            faixa=new LinearLayout(getContext());
            faixa.setOrientation(LinearLayout.HORIZONTAL);
            faixa.setClipChildren(false);
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
        final int limite=Math.min(exibidos.size(),
                limitesPorFileira.containsKey(chave)?limitesPorFileira.get(chave):32);
        int n=0;
        for(Video video:exibidos){
            if(n>=limite)break;
            final int pos=n++;
            final String referencia=chave+"|"+(video.videoId==null?
                    "id:"+video.id:"vid:"+video.videoId);
            final boolean cartaz=estaNaHome();
            final boolean descoberto=chave.startsWith("hx/tema/");
            LinearLayout cartao=new LinearLayout(getContext());
            cartao.setOrientation(LinearLayout.VERTICAL);
            foco(cartao);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(
                    dp(cartaz?LARGURA_CAPA_HOME:220),dp(cartaz?ALTURA_CAPA_HOME:174));
            cp.rightMargin=dp(11);faixa.addView(cartao,cp);
            // Selo sobre a miniatura verdadeira, sem mascarar a interface.
            FrameLayout capa=new FrameLayout(getContext());
            cartao.addView(capa,new LinearLayout.LayoutParams(-1,
                    dp(cartaz?115:123)));
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
            TextView nomeCartao=texto(video.getTitle(),12,TEXTO,true);
            nomeCartao.setMaxLines(2);
            nomeCartao.setEllipsize(TextUtils.TruncateAt.END);
            margem(nomeCartao,8,5,8,0);
            cartao.addView(nomeCartao,new LinearLayout.LayoutParams(-1,0,1));
            TextView dadosCartao=texto(video.getSecondTitle(),9,SECUNDARIO,false);
            dadosCartao.setSingleLine(true);
            dadosCartao.setEllipsize(TextUtils.TruncateAt.END);
            margem(dadosCartao,8,0,8,7);
            cartao.addView(dadosCartao,new LinearLayout.LayoutParams(-1,dp(24)));
            cartoesPorId.put(referencia,cartao);
            if(principal)primeiraFileira.add(cartao);
            cartao.setOnFocusChangeListener((v,selecionado)->{
                if(selecionado)setPainelAberto(false);
                v.setBackground(fundo(selecionado?0xFF272D39:PAINEL,
                        selecionado?0xFFF0F4F9:0,12));
                v.animate().cancel();
                v.animate().scaleX(selecionado?1.018f:1f)
                        .scaleY(selecionado?1.018f:1f)
                        .translationY(selecionado?-dp(2):0)
                        .setDuration(160).start();
                if(Build.VERSION.SDK_INT>=21)
                    v.setElevation(dp(selecionado?4:0));
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
                    if(estaNaHome())atualizarDestaque(video);
                    if(pos>=limite-3 && limite<exibidos.size()){
                        // Carrega mais capas desta mesma fileira conforme necessário.
                        // Redesenho adiado para preservar o vídeo focado pelo controle.
                        int novo=Math.min(exibidos.size(),limite+18);
                        Integer ja=limitesPorFileira.get(chave);
                        if(ja==null||ja<novo){
                            limitesPorFileira.put(chave,novo);
                            postDelayed(()->{
                                if(faixaAtual.isAttachedToWindow())
                                    desenharFileira(chave,nome,exibidos,principal);
                            },160);
                        }
                    }else if(pos>=limite-1){
                        if(descoberto)carregarProximosAssuntos(3);
                        else if(!chave.startsWith("hx/live/"))
                            dados.onScrollEnd(video);
                    }
                }
            });
            // Rotas explícitas na grade; não dependem de FocusFinder atravessar
            // múltiplos HorizontalScrollViews em diferentes versões Android.
            cartao.setOnKeyListener((v,key,event)->{
                if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;
                if(key==KeyEvent.KEYCODE_DPAD_LEFT && pos==0)
                    return focarCategoriaAtual();
                if(key==KeyEvent.KEYCODE_DPAD_UP && principal && !estaNaHome())
                    return destaqueFocavel!=null
                            && destaqueFocavel.requestFocus();
                if(key==KeyEvent.KEYCODE_DPAD_DOWN
                        || (key==KeyEvent.KEYCODE_DPAD_UP && !principal)){
                    List<LinearLayout> linhas=new ArrayList<>(fileiras.values());
                    // A ordem visual muda quando resultados ao vivo chegam.
                    // Ordenar pelas posições reais em vez de ordem de download.
                    Collections.sort(linhas,(x,y)->
                            Integer.compare(posicaoFaixa(x),posicaoFaixa(y)));
                    int atualLinha=linhas.indexOf(faixaAtual);
                    int destino=atualLinha+(key==KeyEvent.KEYCODE_DPAD_DOWN?1:-1);
                    if(destino>=0 && destino<linhas.size()){
                        LinearLayout outra=linhas.get(destino);
                        if(outra.getChildCount()>0){
                            View proximo=outra.getChildAt(
                                    Math.min(pos,outra.getChildCount()-1));
                            if(proximo!=null&&proximo.requestFocus())return true;
                        }
                    }else if(key==KeyEvent.KEYCODE_DPAD_UP
                            &&destaqueFocavel!=null){
                        return destaqueFocavel.requestFocus();
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
    private int posicaoFaixa(LinearLayout faixa){
        View bloco=faixa;
        while(bloco.getParent() instanceof View
                && bloco.getParent()!=pagina)bloco=(View)bloco.getParent();
        if(bloco.getParent()!=pagina)return 100000;
        return pagina.indexOfChild(bloco);
    }
    private void desenharGrupo(VideoGroup grupo){
        if(grupo==null)return;
        if(grupo.getAction()==VideoGroup.ACTION_REPLACE)limparConteudo();
        String id=chave(grupo);
        if(grupo.getAction()==VideoGroup.ACTION_REMOVE){
            grupos.remove(id);
            idsNaTela.remove(id);
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
        if(principal && podeExibirHubAoVivo()){
            final int geracao=geracaoPagina;
            if(!hubAoVivoPronto)
                postDelayed(()->{
                    if(geracao==geracaoPagina)adicionarHubAoVivo();
                },170);
            if(estaNaHome()&&!exploracaoPronta)
                postDelayed(()->{
                    if(geracao==geracaoPagina)adicionarExplorar();
                },400);
        }
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
    @Override
    protected void onDetachedFromWindow(){
        geracaoPagina++;
        for(Disposable d:buscasAssuntos)if(d!=null&&!d.isDisposed())d.dispose();
        buscasAssuntos.clear();
        if(animacaoNav!=null)animacaoNav.cancel();
        super.onDetachedFromWindow();
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
