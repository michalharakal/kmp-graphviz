from graphviz import Digraph

# Criando o diagrama de classes UML com base no cenário fornecido
dot = Digraph(comment='Diagrama de Classes UML - Locadora de Veículos')

# Classe Cliente
dot.node('Cliente', '''Cliente
- nome: String
- cpf: String
- telefone: String
- email: String
+ realizarLocacao()
''')

# Classe Funcionario
dot.node('Funcionario', '''Funcionario
- nome: String
- id: int
- cargo: String
+ atenderCliente()
+ registrarLocacao()
''')

# Classe abstrata Veiculo
dot.node('Veiculo', '''<<abstract>> Veiculo
- modelo: String
- marca: String
- placa: String
- ano: int
- categoria: String
- status: String
+ verificarDisponibilidade()
''')

# Subclasses de Veiculo
dot.node('Carro', 'Carro')
dot.node('Moto', 'Moto')
dot.node('Caminhao', 'Caminhao')

# Herança
dot.edge('Carro', 'Veiculo', arrowhead='empty')
dot.edge('Moto', 'Veiculo', arrowhead='empty')
dot.edge('Caminhao', 'Veiculo', arrowhead='empty')

# Classe Locacao
dot.node('Locacao', '''Locacao
- dataInicio: Date
- dataFim: Date
- valor: float
+ calcularValor()
''')

# Classe Pagamento
dot.node('Pagamento', '''Pagamento
- tipo: String
- status: String
''')

# Relacionamentos
dot.edge('Cliente', 'Locacao', label='1..*')
dot.edge('Veiculo', 'Locacao', label='1..*')
dot.edge('Locacao', 'Pagamento', label='1')
dot.edge('Funcionario', 'Locacao', label='1..*')

# Renderizar e salvar
output_path = "/mnt/data/Diagrama_Classes_Locadora_Veiculos"
dot.render(output_path, format='pdf', cleanup=True)

output_path + ".pdf"/// @file
/// @brief reads graphs with @ref gvNextInputGraph and renders with
/// @ref gvLayoutJobs, @ref gvRenderJobs

/*************************************************************************
 * Copyright (c) 2011 AT&T Intellectual Property
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Details at https://graphviz.org
 *************************************************************************/

#include <graphviz/gvc.h>
#include <stddef.h>

int main(int argc, char **argv) {
  GVC_t *gvc = gvContext();
  gvParseArgs(gvc, argc, argv);

  graph_t *g, *prev = NULL;
  while ((g = gvNextInputGraph(gvc))) {
    if (prev) {
      gvFreeLayout(gvc, prev);
      agclose(prev);
    }
    gvLayoutJobs(gvc, g);
    gvRenderJobs(gvc, g);
    prev = g;
  }
  return gvFreeContext(gvc);
}
