/*
list is [ 1,0,0,1,1,0,0,0,1,0,0,0,1,0,0,0]  etc.

x = XOX([ 1,0,0,1,1,0,0,0,1,0,0,0,1,0,0,0] )
x.asStream


any change to the table causes it to recalc the deltaTable & nextTable

and the stream returns deltas
you make live changes by xox.put(i,1)

its possible to set the position while playing and it will correctly return a delta till the next X
*/

XOX     { // as in roland 707 808 909 and 727
    var <>list,<>repeats;
    var    <stepsize=0.25,  next = 0;
    var <deltaTable,nextTable;

    *new    {    arg list,stepsize=0.25, repeats=inf, step=1.0;
            ^super.new.list_(list).repeats_(repeats)   
                .initxox(stepsize)
    }
   
    initxox { arg st;
        if(st.notNil,{ stepsize=st; });
        nextTable=Array.newClear(list.size);
        deltaTable=nextTable.copy;
        
        this.initTables;
    }

    asStream { // return beats deltas
        next = next ? 0;
        ^Routine
        ({    
        		var position;
            next = next ? 0;
            repeats.value.do
            ({    arg j;
                list.do({
                    position = next;
                    // most recently created gui
                    //viewController.value(position);
                    next=nextTable.at(position);
                    deltaTable.at(position).yield
                })
            })
        })
    }

    position { ^next }
    length { ^list.size + 1 }
   
    initTables {
        var mynext, delta;
        list.do({arg l,i;
            mynext=this.findNextAfter(i);
            if(mynext.notNil,{
                delta = (((mynext-i).wrap(0,list.size) * stepsize ));
                if(delta<=0,{delta=stepsize * list.size});// one bar long
            },{    // nothing playing at all
                // one bar long for now
                mynext=i;
                delta=stepsize * list.size;
            });
            deltaTable.put(i,delta);
            nextTable.put(i,mynext);
        });
    }

    findNextAfter {arg afterme;
        var index;
        list.size.do({arg i;
            index=(i + afterme + 1).wrap(0,list.size).postln;
            if((list@@index).isStrictlyPositive,{^index})
        })
        ^nil
    }
     
    put { arg i,val;
        list.put(i,val);
        this.initTables;

        this.changed;   
    }

}


