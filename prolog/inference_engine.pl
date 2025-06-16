% Vers�o preparada para lidar com regras que contenham nega��o (nao)
% Metaconhecimento
% Usar base de conhecimento veIculos2.txt
% Explica��es como?(how?) e porque n�o?(whynot?)

:-op(220,xfx,entao).
:-op(35,xfy,se).
:-op(240,fx,regra).
:-op(500,fy,nao).
:-op(600,xfy,e).

:- dynamic justifica/3.
:- use_module(library(simplex)).


load_tree:-
		write('Insert, in quotes, the name of the knowledge base (end with .)-> '),
		read(NBC),
		consult(NBC).

start_engine:-	facto(N,Facto),
		facto_dispara_regras1(Facto, LRegras),
		dispara_regras(N, Facto, LRegras),
		ultimo_facto(N).

facto_dispara_regras1(Facto, LRegras):-
	facto_dispara_regras(Facto, LRegras),
	!.
facto_dispara_regras1(_, []).
% Caso em que o facto n�o origina o disparo de qualquer regra.

dispara_regras(N, Facto, [ID|LRegras]):-
	regra ID se LHS entao RHS,
	facto_esta_numa_condicao(Facto,LHS),
	% Instancia Facto em LHS
	verifica_condicoes(LHS, LFactos),
	member(N,LFactos),
	concluir(RHS,ID,LFactos),
	!,
	dispara_regras(N, Facto, LRegras).

dispara_regras(N, Facto, [_|LRegras]):-
	dispara_regras(N, Facto, LRegras).

dispara_regras(_, _, []).


facto_esta_numa_condicao(F,[F  e _]).

facto_esta_numa_condicao(F,[avalia(F1)  e _]):- F=..[H,H1|_],F1=..[H,H1|_].

facto_esta_numa_condicao(F,[_ e Fs]):- facto_esta_numa_condicao(F,[Fs]).

facto_esta_numa_condicao(F,[F]).

facto_esta_numa_condicao(F,[avalia(F1)]):-F=..[H,H1|_],F1=..[H,H1|_].


verifica_condicoes([nao avalia(X) e Y],[nao X|LF]):- !,
	\+ avalia(_,X),
	verifica_condicoes([Y],LF).
verifica_condicoes([avalia(X) e Y],[N|LF]):- !,
	avalia(N,X),
	verifica_condicoes([Y],LF).

verifica_condicoes([nao avalia(X)],[nao X]):- !, \+ avalia(_,X).
verifica_condicoes([avalia(X)],[N]):- !, avalia(N,X).

verifica_condicoes([nao X e Y],[nao X|LF]):- !,
	\+ facto(_,X),
	verifica_condicoes([Y],LF).
verifica_condicoes([X e Y],[N|LF]):- !,
	facto(N,X),
	verifica_condicoes([Y],LF).

verifica_condicoes([nao X],[nao X]):- !, \+ facto(_,X).
verifica_condicoes([X],[N]):- facto(N,X).


concluir([cria_facto(F)|Y],ID,LFactos):-
	!,
	cria_facto(F,ID,LFactos),
	concluir(Y,ID,LFactos).

concluir([],_,_):-!.

cria_facto(F,_,_):-
	facto(_,F),!.

cria_facto(F,ID,LFactos):-
	retract(ultimo_facto(N1)),
	N is N1+1,
	asserta(ultimo_facto(N)),
	assertz(justifica(N,ID,LFactos)),
	F,
	assertz(facto(N,F)),
	write('Concluded fact number '),write(N),write(' -> '),write(F),get0(_),!.



avalia(N,P):-	P=..[Functor,Entidade,Operando,Valor],
		P1=..[Functor,Entidade,Valor1],
		facto(N,P1),
		compara(Valor1,Operando,Valor).

compara(V1,==,V):- V1==V.
compara(V1,\==,V):- V1\==V.
compara(V1,>,V):-V1>V.
compara(V1,<,V):-V1<V.
compara(V1,>=,V):-V1>=V.
compara(V1,=<,V):-V1=<V.

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Setup

ask_disp(Answer) :-
	write("Esta disposto a mudar a potência contratada?"),
	read(Answer),
	assertz(facto(1, switch_contract(Answer))).

ask_user_nr(Answer) :-
	write("Qual o número de utilizador?"),
	read(Answer),
	% ir já buscar os dados do user?
	assertz(facto(2, user_nr(Answer))).

ask_pot_contr(Answer) :-
	write("Qual é a sua potência contratada?"),
	read(Answer),
	assertz(facto(3, energy_contract(Answer))).

ask_pot_bi(Answer) :-
	write("Potencia contratada é bi-horário?"),
	read(Answer),
	assertz(facto(4, bi_schedule(Answer))).

% "Esta disposto a mudar a potência contratada"
% "Qual o número de utilizador"
% "Potencia contratada é bi-horário?"
% 	Se não for -> Se 40% do consumo dia <= consumo da noirte -> bihorario
% "Está disposto a investir em paineis fotovoltaicos?"
% "Tem espaço para instalação de painéis virados a sul?"
% "Está disposto a injetar/vender energia a um agregador ou vizinhos"
% 	Sim -> procura maximo paineis a instalar -> dá a energia que produz
% 	Não -> instala o minimo para cobrir o consumo das 12 às 14

% "Está disposto a investir na troca de eletrodomésticos"
% 	Para cada eletrodoméstico com consumo (QUE EXISTE) -> Perguntar a eficiência.
% 	Perguntar se o equipamento é programável (Maq lavar loiça/roupa).

% "Se bi-horário==False -> Perguntar se equipamentos são programáveis"
	


% Calculation
% X = [1.15, 2.30, 3.45, 4.60, 5.75]
calculate_contract(Initial_contracted_power, Max_consuption, S) :-
	best_fit([1.15,2.30,3.45,4.60,5.75], Initial_contracted_power, Max_consuption, Solution),
	Final_contracted_power is Solution.

best_fit([X|[]], Initial_contracted_power, Max_consuption, Solution) :-
	Initial_contracted_power > X,
	Max_consuption < X,
	Solution is X.

best_fit([H|T], Initial_contracted_power, Max_consuption, Solution) :-
	Initial_contracted_power > H,
	Max_consuption < H,
	Solution is H,
	!.

best_fit([H|T], Initial_contracted_power, Max_consuption, Solution) :-
	best_fit(T, Initial_contracted_power, Max_consuption, Solution).


ask_user(Question, Answer) :-
	write(Question),
	read(Answer).



%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Visualiza��o da base de factos

show_facts:-
	findall(N, facto(N, _), LFactos),
	escreve_factos(LFactos).


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Gera��o de explica��es do tipo "Como"

how(N):-ultimo_facto(Last),Last<N,!,
	write('Not yet concluded such fact'),nl,nl.
how(N):-justifica(N,ID,LFactos),!,
	facto(N,F),
	write('Concluded fact number '),write(N),write(' -> '),write(F),nl,
	write('with the rule '),write(ID),nl,
	write('since it was observed that:'),nl,
	escreve_factos(LFactos),
	write('********************************************************'),nl,
	explica(LFactos).
how(N):-facto(N,F),
	write('Fact number '),write(N),write(' -> '),write(F),nl,
	write('was initially known.'),nl,
	write('********************************************************'),nl.


escreve_factos([I|R]):-facto(I,F), !,
	write('Fact number '),write(I),write(' -> '),write(F),write(' is True'),nl,
	escreve_factos(R).
escreve_factos([I|R]):-
	write('The condition '),write(I),write(' is True'),nl,
	escreve_factos(R).
escreve_factos([]).

explica([I|R]):- \+ integer(I),!,explica(R).
explica([I|R]):- how(I),
		explica(R).
explica([]):-	write('********************************************************'),nl.




%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Gera��o de explica��es do tipo "Porque nao"
% Exemplo: ?- whynot(classe(meu_veiculo,ligeiro)).

whynot(Facto):-
	whynot(Facto,1).

whynot(Facto,_):-
	facto(_, Facto),
	!,
	write('Fact '),write(Facto),write(' is not False!'),nl.
whynot(Facto,Nivel):-
	encontra_regras_whynot(Facto,LLPF),
	whynot1(LLPF,Nivel).
whynot(nao Facto,Nivel):-
	formata(Nivel),write('Why:'),write(' Fact '),write(Facto),
	write(' is True.'),nl.
whynot(Facto,Nivel):-
	formata(Nivel),write('Why:'),write(' Fact '),write(Facto),
	write(' is not defined in the knowledge base.'),nl.

%  As explica��es do whynot(Facto) devem considerar todas as regras que poderiam dar origem a conclus�o relativa ao facto Facto

encontra_regras_whynot(Facto,LLPF):-
	findall((ID,LPF),
		(
		regra ID se LHS entao RHS,
		member(cria_facto(Facto),RHS),
		encontra_premissas_falsas(LHS,LPF),
		LPF \== []
		),
		LLPF).

whynot1([],_).
whynot1([(ID,LPF)|LLPF],Nivel):-
	formata(Nivel),write('Becasue by the rule '),write(ID),write(':'),nl,
	Nivel1 is Nivel+1,
	explica_porque_nao(LPF,Nivel1),
	whynot1(LLPF,Nivel).

encontra_premissas_falsas([nao X e Y], LPF):-
	verifica_condicoes([nao X], _),
	!,
	encontra_premissas_falsas([Y], LPF).
encontra_premissas_falsas([X e Y], LPF):-
	verifica_condicoes([X], _),
	!,
	encontra_premissas_falsas([Y], LPF).
encontra_premissas_falsas([nao X], []):-
	verifica_condicoes([nao X], _),
	!.
encontra_premissas_falsas([X], []):-
	verifica_condicoes([X], _),
	!.
encontra_premissas_falsas([nao X e Y], [nao X|LPF]):-
	!,
	encontra_premissas_falsas([Y], LPF).
encontra_premissas_falsas([X e Y], [X|LPF]):-
	!,
	encontra_premissas_falsas([Y], LPF).
encontra_premissas_falsas([nao X], [nao X]):-!.
encontra_premissas_falsas([X], [X]).
encontra_premissas_falsas([]).

explica_porque_nao([],_).
explica_porque_nao([nao avalia(X)|LPF],Nivel):-
	!,
	formata(Nivel),write('Condition '),write(X),write(' is not False'),nl,
	explica_porque_nao(LPF,Nivel).
explica_porque_nao([avalia(X)|LPF],Nivel):-
	!,
	formata(Nivel),write('Condition '),write(X),write(' is False'),nl,
	explica_porque_nao(LPF,Nivel).
explica_porque_nao([P|LPF],Nivel):-
	formata(Nivel),write('Premise '),write(P),write(' is False'),nl,
	Nivel1 is Nivel+1,
	whynot(P,Nivel1),
	explica_porque_nao(LPF,Nivel).

formata(Nivel):-
	Esp is (Nivel-1)*5, tab(Esp).
