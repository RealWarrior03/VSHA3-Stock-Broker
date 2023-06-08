# How to excute our code
- soon

# To-Do
- [ ] provide access to at least three different stocks
  - consisting of a name, a price, number of available units and maximum number of units
- [ ] handle a registration queue for clients
- [ ] handle one input and one output queue per client
- [ ] receive control commands on the incoming queues
- [ ] send notifications/results on the outgoing queues
- [ ] handle multiple (parallel) clients
  - synchronize access to data structures as necessary
- Handle the following commands:
  - [ ] Register
    - a client registers via the registration queue; Following the registration, a dedicated input and output queue are generated for communication with the client
  - [ ] List stocks
    - on request from a client, the server answers with a list of the stocks; the answer can be asynchronous, i.e. a simple message on the outgoing queue. The list shows the available number of stocks and its prices.
  - [ ] Buy stocks
    - allow clients to buy stocks, as long as the stocks are available
  - [ ] Sell stocks
    - allow clients to sell stocks, as long as they were previously purchased by the same client
- [ ] Handle one topic per stock

# Bugs
- nothing so far